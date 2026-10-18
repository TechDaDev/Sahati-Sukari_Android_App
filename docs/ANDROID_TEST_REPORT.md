# Android test report

Verification run: 2026-10-18. JDK 17, Android Gradle Plugin 8.13.2, Gradle 8.13.

| Gate | Result | Evidence |
|---|---|---|
| `testDebugUnitTest` | PASS | Existing API contract tests and new health-profile presentation tests passed. See Gradle XML results for exact count. |
| `assembleDebug` | PASS | `app/build/outputs/apk/debug/app-debug.apk` generated (debug-signed). |
| `lintDebug` | PASS | Gradle task completed successfully. |
| `connectedDebugAndroidTest` | PASS | 5 Compose tests passed on `m30_ops_api35` at 360 × 800 dp: login/emergency, six-item navigation sizing/alignment, increased font scale, localized profile display, and condition editing/payload preservation. |
| Authenticated API smoke test | PASS | Supplied credential file used for login (HTTP 201); read-only `GET /dashboard` and `GET /profile` returned HTTP 200. Response bodies and credential values were suppressed. No API mutation endpoint was called. |
| Emulator viewport | PASS, limited | AVD set to 720 × 1600 px at 320 dpi (360 × 800 dp). |
| Visual screenshot comparison | NOT VERIFIED | App screenshot framebuffer remained black (zero RGB variance), while UI hierarchy exposed Arabic login strings. Dashboard comparison against supplied website screenshot could not be performed. |
| Smaller/larger viewport review | NOT VERIFIED | Automated Compose checks covered 360 dp width and 1.35 font scale; no screenshot review at other widths. |

Unit tests cover auth/session response parsing, registration payload including national ID consent, refresh request ID and recovery contract, glucose creation payload, cursor query parameters, record deletion path, and profile presentation/condition mapping. They do not replace visual review or live write-flow verification.

The supplied `docs/credential.md` was used for authenticated read-only smoke testing. It is excluded from the correction commit and must not be copied into reports or artifacts. No profile, measurement, medication, or other patient-data mutation was issued. No production backend code or configuration was changed.

Do not treat visual parity or authenticated write flows as passed.
