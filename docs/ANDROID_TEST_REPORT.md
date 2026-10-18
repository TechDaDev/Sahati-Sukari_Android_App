# Android test report

Verification run: 2026-10-18. JDK 17, Android Gradle Plugin 8.13.2, Gradle 8.13.

| Gate | Result | Evidence |
|---|---|---|
| `testDebugUnitTest` | PASS | 6 MockWebServer contract tests passed across auth and patient APIs. |
| `assembleDebug` | PASS | `app/build/outputs/apk/debug/app-debug.apk` generated (22 MB, debug-signed). |
| `lintDebug` | PASS | Gradle task completed successfully. |
| `connectedDebugAndroidTest` | PASS | 1 Compose UI test on `m30_ops_api35` verified Arabic login entry and emergency action. |
| Emulator viewport | PASS, limited | AVD override set to 720×1600 px at 320 dpi (360×800 dp). |
| Visual screenshot comparison | NOT VERIFIED | UI hierarchy exposes Arabic login text, but the emulator screenshot framebuffer is black. Dashboard comparison against the supplied website screenshot could not be performed. |
| Live authenticated backend flows | NOT RUN | No synthetic account was supplied. No production patient data was used. |

Unit tests cover login/session response parsing, registration payload including required national ID consent, refresh request ID and recovery contract, glucose creation payload, cursor query parameters, and record deletion path. They do not replace end-to-end verification of every screen or live API workflow.

No database or production backend changes were made. Do not treat visual parity or live authenticated flows as passed.
