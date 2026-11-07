package sa.factory.maqaraccesscoworki

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.maqaraccesscoworki.data.repository.MaqarAccessRepositoryImpl
import sa.factory.maqaraccesscoworki.engine.*
import sa.factory.maqaraccesscoworki.hardware.HardwareSensorDriver
import sa.factory.maqaraccesscoworki.model.MaqarAccessItem

class MaqarAccessRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = MaqarAccessRepositoryImpl(sensorDriver)

        val item1 = MaqarAccessItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = MaqarAccessItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testRfidAuthorizedTurnstileAccess() {
        val engine = RfidTurnstileAccessEngine()
        val result = engine.evaluateAccess("04:A2:3B:5C:6D:7E:80", AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = true)

        assertTrue("Access should be granted to valid member", result.isSuccess)
        assertEquals(TurnstileGateState.ACCESS_GRANTED, result.gateState)
        assertEquals(150L, result.relayPulseDurationMs)
        assertEquals("SHA-256 audit digest must be 64 hex characters", 64, result.auditLogDigest.length)
    }

    @Test
    fun testRfidUnauthorizedZoneDenied() {
        val engine = RfidTurnstileAccessEngine()
        // Sara Al-Dosari has Hot Desk (Main + Quiet Pod), not Server Room
        val result = engine.evaluateAccess("04:11:22:33:44:55:66", AccessZoneMask.SERVER_ROOM, isEntryTurnstile = true)

        assertFalse("Access should be denied to unauthorized zone", result.isSuccess)
        assertEquals(TurnstileGateState.ACCESS_DENIED, result.gateState)
        assertEquals(0L, result.relayPulseDurationMs)
        assertTrue(result.message.contains("Unauthorized zone"))
    }

    @Test
    fun testRfidAntiPassbackViolation() {
        val engine = RfidTurnstileAccessEngine()
        val cardUid = "04:A2:3B:5C:6D:7E:80"

        // First entry: Granted
        val firstEntry = engine.evaluateAccess(cardUid, AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = true)
        assertTrue(firstEntry.isSuccess)

        // Second consecutive entry without exit: Anti-Passback Alarm
        val secondEntry = engine.evaluateAccess(cardUid, AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = true)
        assertFalse("Second consecutive entry must be blocked by Anti-Passback", secondEntry.isSuccess)
        assertEquals(TurnstileGateState.ALARM_ANTI_PASSBACK, secondEntry.gateState)

        // Exit: Granted
        val exit = engine.evaluateAccess(cardUid, AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = false)
        assertTrue("Exit should be permitted", exit.isSuccess)
    }

    @Test
    fun testRfidExpiredMembershipDenied() {
        val engine = RfidTurnstileAccessEngine()
        val expiredBadge = RfidBadge(
            cardUid = "04:99:99:99:99:99:99",
            memberId = "MBR-EXPIRED",
            memberName = "Tariq Ali",
            tier = CoworkingMembershipTier.DAY_PASS,
            allowedZonesMask = AccessZoneMask.MAIN_TURNSTILE,
            expiryTimestamp = System.currentTimeMillis() - 10000L // Expired 10s ago
        )
        engine.registerBadge(expiredBadge)

        val result = engine.evaluateAccess("04:99:99:99:99:99:99", AccessZoneMask.MAIN_TURNSTILE, isEntryTurnstile = true)
        assertFalse("Expired pass must be rejected", result.isSuccess)
        assertTrue(result.message.contains("expired"))
    }

    @Test
    fun testTotpPassGenerationAndVerification() {
        val engine = SmartOfficeTotpEngine()
        val secret = "48656c6c6f21deadbeefcafe0123456789abcdef"
        val pass = engine.generateTotp(secret)

        assertNotNull(pass.tokenCode)
        assertEquals(6, pass.tokenCode.length)
        assertTrue("Remaining seconds should be between 1 and 30", pass.remainingSeconds in 1..30)
        assertTrue("QR payload must include token code", pass.qrPayload.contains(pass.tokenCode))

        val isValid = engine.verifyTotp(pass.tokenCode, secret)
        assertTrue("Generated TOTP code must pass mathematical verification", isValid)

        val isInvalid = engine.verifyTotp("999999", secret)
        assertFalse("Random token should fail verification", isInvalid)
    }

    @Test
    fun testBleProximityAndMeetingRoomAutoRelease() {
        val engine = SmartOfficeTotpEngine()

        // BLE Proximity test
        assertTrue("RSSI -50 dBm is near (< 1.5m)", engine.isBleProximityValid(-50))
        assertFalse("RSSI -75 dBm is far (> 2m)", engine.isBleProximityValid(-75))

        // Meeting room auto-release test
        val startTime = System.currentTimeMillis() - 15 * 60 * 1000L // 15 mins ago
        val endTime = startTime + 60 * 60 * 1000L
        val unconfirmedBooking = MeetingRoomBooking(
            bookingId = "BK-101",
            roomId = "ROOM-MAJLIS",
            roomName = "Al-Majlis Boardroom",
            reservedByMemberId = "MBR-9041",
            startTimestamp = startTime,
            endTimestamp = endTime,
            isCheckedIn = false
        )

        val evaluated = engine.evaluateBookingStatus(unconfirmedBooking, System.currentTimeMillis())
        assertTrue("Unchecked booking after 15m must be auto-released", evaluated.isAutoReleased)
    }

    @Test
    fun testHardwareSensorDriverEmitsValidTelemetry() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("ONLINE"))
        assertTrue(frames[1].livePayload.contains("RFID: Ready"))
        assertTrue(frames[1].livePayload.contains("TOTP:"))
    }
}
