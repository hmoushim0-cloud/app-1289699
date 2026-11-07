# تطبيق مقر الذكي — تعليم على التسويق عبر ميتا و (`maqar-access-coworking-1289699`)
> منظومة coworking المتكاملة مع ربط 13.56 MHz RFID Turnstile Access + TOTP Pass

- **Project ID**: `#1289699`
- **Commercial Archetype**: `coworking`
- **Bound Hardware Peripheral**: `13.56 MHz RFID Turnstile Access + TOTP Pass`
- **Package Name**: `sa.factory.maqaraccesscoworki`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### Implemented Production Engines & Hardware Binding
1. **RfidTurnstileAccessEngine (`sa.factory.maqaraccesscoworki.engine.RfidTurnstileAccessEngine`)**:
   - Supports 13.56 MHz High-Frequency RFID and NFC (ISO/IEC 14443 Type A, MIFARE Classic & DESFire EV2/EV3).
   - Validates multi-tier membership access privileges (Hot Desk, Dedicated Desk, Private Office Suite, Enterprise Flex).
   - Enforces facility zone security bitmasks (Main Turnstile, Quiet Work Pods, Meeting Rooms, Podcast Studio, Server Room).
   - Implements anti-passback (APB) state machine with automated 150ms pulsed barrier relay actuation and SHA-256 audit logging.

2. **SmartOfficeTotpEngine (`sa.factory.maqaraccesscoworki.engine.SmartOfficeTotpEngine`)**:
   - Generates and verifies dynamic RFC 6238 Time-based One-Time Passwords with HMAC-SHA256 rotating every 30 seconds.
   - Encodes contactless digital QR passes for touchless smartphone gate turnstile presentation.
   - Meeting Room IoT booking occupancy engine: detects BLE proximity (-65 dBm near threshold) and executes automated 10-minute no-show room release.

3. **Hardware & UI Hardening**:
   - Integrated live reactive StateFlow telemetry stream in `HardwareSensorDriver.kt`.
   - Resolved Kotlin single-quote syntax issues in string templates in `HomeScreen.kt` and `TelemetryScreen.kt`.
   - Added missing Compose typography and unit imports in `MainActivity.kt`.
   - Cleaned UI badges to strictly enforce zero emoji and ASCII compliance.
   - Relocated regression test suite to `app/src/test/java/sa/factory/maqaraccesscoworki/` with 8 comprehensive unit tests (100% assertions green).

### Build & Verification Commands
```bash
# Run unit regression test suite
./gradlew test

# Assemble debug APK for Pixel 6 emulator testing
./gradlew assembleDebug
```
