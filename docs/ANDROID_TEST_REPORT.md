# Android test report

Verification run: 2026-10-18. JDK 17, Android Gradle Plugin 8.13.2, Gradle 8.13.

## Current release-candidate run

Run started 2026-10-22 with JDK 17. The initial `testDebugUnitTest` run found one fixture error: it removed optional `releaseNotes` and expected rejection. The fixture now removes required `versionName`.

| Current gate | Result | Evidence |
|---|---|---|
| `clean` | PASS | Clean Gradle task completed in JDK 17 run. |
| `testDebugUnitTest` | PASS | Final run: 17 tests passed, 0 failures. |
| `lintDebug` | PASS | Final isolated run completed. A combined lint/build run hit a missing release Hilt generated file; rerun alone passed. |
| `assembleDebug` / `assembleRelease` | PASS | Final variants built; release APK is unsigned. |
| `connectedDebugAndroidTest` | PASS | Final run: 5 tests passed on API 35 `sahati_api35_visible`: login/emergency, six destinations, increased font scale, profile filtering, condition edit. |
| Added Health Profile history fixtures | PASS | Tests cover all 3 weight fields and all 7 website lab fields using synthetic JSON. |
| Runtime screenshot | PASS, limited | Arabic login screen visually reviewed at 360 × 800 dp using SwiftShader; saved under `docs/screenshots/`. Authenticated screens not captured. |
| Release signing | BLOCKED | Final `app-release-unsigned.apk`; `apksigner` reports `DOES NOT VERIFY`. |
| Public hosting | BLOCKED | APK and metadata URLs return HTTP 404. |

The release candidate changes version to `0.2.0` / code `2`, adds strict manifest validation and browser-only update prompts, and adds optional external release signing inputs. No approved signing key was found. Do not treat an unsigned release APK as production-ready.

Public HEAD checks returned HTTP 404 for both configured distribution URLs on 2026-10-22. Namecheap MCP exposes DNS only; hosting file access and upload are unavailable through connected tools.

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
