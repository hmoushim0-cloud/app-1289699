package sa.factory.maqaraccesscoworki.engine

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Dynamic TOTP Digital Access Pass.
 */
data class TotpAccessPass(
    val memberId: String,
    val tokenCode: String,
    val remainingSeconds: Int,
    val qrPayload: String,
    val epochWindow: Long
)

/**
 * Meeting Room booking session with IoT presence tracking.
 */
data class MeetingRoomBooking(
    val bookingId: String,
    val roomId: String,
    val roomName: String,
    val reservedByMemberId: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isCheckedIn: Boolean = false,
    val isAutoReleased: Boolean = false
)

/**
 * Smart Office TOTP & Meeting Room Smart BLE Lock Actuation Engine.
 */
class SmartOfficeTotpEngine {

    companion object {
        const val TIME_STEP_SECONDS = 30L
        const val AUTO_RELEASE_GRACE_PERIOD_MS = 10 * 60 * 1000L // 10 minutes
        const val BLE_NEAR_RSSI_THRESHOLD = -65 // dBm (approx 1.5m)
    }

    /**
     * Generates standard RFC 6238 TOTP code (6 decimal digits) from shared key.
     */
    fun generateTotp(secretKeyHex: String, timestampMs: Long = System.currentTimeMillis()): TotpAccessPass {
        val epochWindow = timestampMs / 1000L / TIME_STEP_SECONDS
        val remainingSec = (TIME_STEP_SECONDS - ((timestampMs / 1000L) % TIME_STEP_SECONDS)).toInt()

        val keyBytes = hexStringToByteArray(secretKeyHex)
        val data = ByteBuffer.allocate(8).putLong(epochWindow).array()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val codeInt = binary % 1000000
        val codeStr = String.format("%06d", codeInt)

        val qrPayload = "MAQAR-PASS::SEC::$codeStr::$epochWindow"

        return TotpAccessPass(
            memberId = "MBR-ACTIVE",
            tokenCode = codeStr,
            remainingSeconds = remainingSec,
            qrPayload = qrPayload,
            epochWindow = epochWindow
        )
    }

    /**
     * Verifies TOTP token with +/- 1 time step clock drift tolerance.
     */
    fun verifyTotp(token: String, secretKeyHex: String, timestampMs: Long = System.currentTimeMillis()): Boolean {
        val currentWindow = timestampMs / 1000L / TIME_STEP_SECONDS
        val keyBytes = hexStringToByteArray(secretKeyHex)
        val mac = Mac.getInstance("HmacSHA256")
        val secretSpec = SecretKeySpec(keyBytes, "HmacSHA256")

        for (window in (currentWindow - 1)..(currentWindow + 1)) {
            val data = ByteBuffer.allocate(8).putLong(window).array()
            mac.init(secretSpec)
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val expected = String.format("%06d", binary % 1000000)
            if (expected == token) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if member is within close BLE lock proximity for hands-free unlock.
     */
    fun isBleProximityValid(rssiDbm: Int): Boolean {
        return rssiDbm >= BLE_NEAR_RSSI_THRESHOLD
    }

    /**
     * Evaluates room occupancy and auto-release policies.
     */
    fun evaluateBookingStatus(booking: MeetingRoomBooking, currentTimestampMs: Long): MeetingRoomBooking {
        // If already checked in or ended, no auto-release
        if (booking.isCheckedIn || currentTimestampMs > booking.endTimestamp) {
            return booking
        }

        // If beyond 10-minute grace period without check-in, release room for other members
        val graceLimit = booking.startTimestamp + AUTO_RELEASE_GRACE_PERIOD_MS
        if (currentTimestampMs > graceLimit && !booking.isCheckedIn) {
            return booking.copy(isAutoReleased = true)
        }

        return booking
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
