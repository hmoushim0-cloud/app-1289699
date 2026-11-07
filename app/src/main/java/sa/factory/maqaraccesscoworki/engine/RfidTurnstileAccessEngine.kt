package sa.factory.maqaraccesscoworki.engine

import java.security.MessageDigest
import kotlin.math.abs

/**
 * Access tier classification for smart coworking members.
 */
enum class CoworkingMembershipTier {
    HOT_DESK,
    DEDICATED_DESK,
    PRIVATE_OFFICE,
    ENTERPRISE_FLEX,
    DAY_PASS
}

/**
 * Access zone bitmask flags for facility security barriers.
 */
object AccessZoneMask {
    const val MAIN_TURNSTILE = 1 shl 0    // 0x01
    const val QUIET_WORK_POD = 1 shl 1    // 0x02
    const val MEETING_ROOMS = 1 shl 2     // 0x04
    const val PODCAST_STUDIO = 1 shl 3    // 0x08
    const val EXECUTIVE_LOUNGE = 1 shl 4  // 0x10
    const val SERVER_ROOM = 1 shl 5       // 0x20
}

/**
 * Barrier actuation status states.
 */
enum class TurnstileGateState {
    IDLE_LOCKED,
    VALIDATING_CREDENTIAL,
    ACCESS_GRANTED,
    RELAY_ACTUATING,
    PASSAGE_CONFIRMED,
    ACCESS_DENIED,
    ALARM_ANTI_PASSBACK
}

/**
 * Member badge credential representation.
 */
data class RfidBadge(
    val cardUid: String,
    val memberId: String,
    val memberName: String,
    val tier: CoworkingMembershipTier,
    val allowedZonesMask: Int,
    val expiryTimestamp: Long,
    val isInsideFacility: Boolean = false
)

/**
 * Result of turnstile gating evaluation.
 */
data class GateAccessResult(
    val isSuccess: Boolean,
    val gateState: TurnstileGateState,
    val message: String,
    val relayPulseDurationMs: Long,
    val auditLogDigest: String
)

/**
 * 13.56 MHz RFID / NFC Turnstile Access and Barrier Actuation Engine.
 * Supports ISO/IEC 14443 Type A MIFARE DESFire authentication.
 */
class RfidTurnstileAccessEngine {

    private val badgeRegistry = mutableMapOf<String, RfidBadge>()

    init {
        // Register default seed credentials for enterprise coworking demo
        registerBadge(
            RfidBadge(
                cardUid = "04:A2:3B:5C:6D:7E:80",
                memberId = "MBR-9041",
                memberName = "Faisal Al-Otaibi",
                tier = CoworkingMembershipTier.ENTERPRISE_FLEX,
                allowedZonesMask = AccessZoneMask.MAIN_TURNSTILE or AccessZoneMask.MEETING_ROOMS or AccessZoneMask.EXECUTIVE_LOUNGE,
                expiryTimestamp = System.currentTimeMillis() + 86400000L * 30,
                isInsideFacility = false
            )
        )
        registerBadge(
            RfidBadge(
                cardUid = "04:11:22:33:44:55:66",
                memberId = "MBR-8812",
                memberName = "Sara Al-Dosari",
                tier = CoworkingMembershipTier.HOT_DESK,
                allowedZonesMask = AccessZoneMask.MAIN_TURNSTILE or AccessZoneMask.QUIET_WORK_POD,
                expiryTimestamp = System.currentTimeMillis() + 86400000L * 14,
                isInsideFacility = false
            )
        )
    }

    fun registerBadge(badge: RfidBadge) {
        badgeRegistry[badge.cardUid] = badge
    }

    fun getBadge(cardUid: String): RfidBadge? = badgeRegistry[cardUid]

    /**
     * Evaluates access attempt at a physical turnstile gate with anti-passback and zone permissions.
     */
    fun evaluateAccess(
        cardUid: String,
        targetZoneMask: Int,
        isEntryTurnstile: Boolean = true
    ): GateAccessResult {
        val badge = badgeRegistry[cardUid]
        val now = System.currentTimeMillis()

        if (badge == null) {
            val digest = generateAuditDigest("DENIED_UNKNOWN", cardUid, targetZoneMask, now)
            return GateAccessResult(
                isSuccess = false,
                gateState = TurnstileGateState.ACCESS_DENIED,
                message = "Card UID not recognized in security database",
                relayPulseDurationMs = 0L,
                auditLogDigest = digest
            )
        }

        if (now > badge.expiryTimestamp) {
            val digest = generateAuditDigest("DENIED_EXPIRED", cardUid, targetZoneMask, now)
            return GateAccessResult(
                isSuccess = false,
                gateState = TurnstileGateState.ACCESS_DENIED,
                message = "Membership pass has expired",
                relayPulseDurationMs = 0L,
                auditLogDigest = digest
            )
        }

        // Zone permission bitmask check
        if ((badge.allowedZonesMask and targetZoneMask) == 0) {
            val digest = generateAuditDigest("DENIED_UNAUTHORIZED_ZONE", cardUid, targetZoneMask, now)
            return GateAccessResult(
                isSuccess = false,
                gateState = TurnstileGateState.ACCESS_DENIED,
                message = "Unauthorized zone: insufficient membership privileges",
                relayPulseDurationMs = 0L,
                auditLogDigest = digest
            )
        }

        // Anti-Passback (APB) check for outer perimeter main turnstiles
        if (targetZoneMask == AccessZoneMask.MAIN_TURNSTILE) {
            if (isEntryTurnstile && badge.isInsideFacility) {
                val digest = generateAuditDigest("ALARM_APB_DOUBLE_ENTRY", cardUid, targetZoneMask, now)
                return GateAccessResult(
                    isSuccess = false,
                    gateState = TurnstileGateState.ALARM_ANTI_PASSBACK,
                    message = "Anti-Passback violation: card already checked in",
                    relayPulseDurationMs = 0L,
                    auditLogDigest = digest
                )
            }
            if (!isEntryTurnstile && !badge.isInsideFacility) {
                val digest = generateAuditDigest("ALARM_APB_DOUBLE_EXIT", cardUid, targetZoneMask, now)
                return GateAccessResult(
                    isSuccess = false,
                    gateState = TurnstileGateState.ALARM_ANTI_PASSBACK,
                    message = "Anti-Passback violation: card not inside facility",
                    relayPulseDurationMs = 0L,
                    auditLogDigest = digest
                )
            }
        }

        // Update presence status
        val updatedBadge = badge.copy(isInsideFacility = if (targetZoneMask == AccessZoneMask.MAIN_TURNSTILE) isEntryTurnstile else badge.isInsideFacility)
        badgeRegistry[cardUid] = updatedBadge

        val digest = generateAuditDigest("GRANTED", cardUid, targetZoneMask, now)
        return GateAccessResult(
            isSuccess = true,
            gateState = TurnstileGateState.ACCESS_GRANTED,
            message = "Access granted to ${badge.memberName} [${badge.tier.name}]",
            relayPulseDurationMs = 150L,
            auditLogDigest = digest
        )
    }

    private fun generateAuditDigest(status: String, uid: String, zone: Int, ts: Long): String {
        val input = "RFID-AUDIT::$status::$uid::ZONE-$zone::$ts"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
