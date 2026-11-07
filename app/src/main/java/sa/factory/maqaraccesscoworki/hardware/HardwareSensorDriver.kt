package sa.factory.maqaraccesscoworki.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.maqaraccesscoworki.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    val peripheral = "13.56 MHz RFID Turnstile Access + TOTP Pass"
    val rfidEngine = RfidTurnstileAccessEngine()
    val totpEngine = SmartOfficeTotpEngine()

    private val masterSecretHex = "48656c6c6f21deadbeefcafe0123456789abcdef" // 20-byte test key

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "13.56 MHz RFID Transceiver & NFC Front-end initializing..."))
        delay(200)

        // Seed initial access evaluation
        val initialPass = totpEngine.generateTotp(masterSecretHex)
        val initialAccess = rfidEngine.evaluateAccess("04:A2:3B:5C:6D:7E:80", AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = true)

        val initPayload = "[ONLINE] RFID: Ready (ISO 14443A) | TOTP: " + initialPass.tokenCode +
                " (" + initialPass.remainingSeconds + "s left) | Gate: " + initialAccess.gateState.name +
                " | Relay: " + initialAccess.relayPulseDurationMs + "ms"
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", initPayload))

        var frameCounter = 1
        while (true) {
            delay(1500)
            frameCounter++
            val jitter = Random.nextInt(1, 8)
            val pass = totpEngine.generateTotp(masterSecretHex)
            val rssi = -55 - Random.nextInt(0, 15)
            val isNear = totpEngine.isBleProximityValid(rssi)

            val liveStatus = "[ACTIVE_STREAMING] Frame #" + frameCounter + " (+" + jitter + "ms) | " +
                    "TOTP: " + pass.tokenCode + " (" + pass.remainingSeconds + "s) | " +
                    "BLE RSSI: " + rssi + " dBm (Prox: " + (if (isNear) "NEAR" else "FAR") + ") | " +
                    "Turnstile: 13.56 MHz ACTIVE"

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", liveStatus))
        }
    }
}
