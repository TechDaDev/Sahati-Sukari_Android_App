# Phase 7 — Final Audit and Android Release Candidate

## Scope and result

Audited the Phase 1–6 Android implementation and built a local, signed release candidate. No confirmed integration regression required a behavior change. The only source change for this release candidate is the version increment in `app/build.gradle.kts`; no production service, update manifest, patient record, or session was modified.

The audit and builds used an isolated source snapshot under `/tmp` so the existing `app/build/outputs/apk/release/app-release.apk` and other build artifacts were not overwritten. Release candidate APKs and credentials are not included in Git.

## Six-phase completion matrix

| Phase | Area | Integration audit | Automated evidence |
|---|---|---|---|
| 1 | Arabic medical presentation | Shared Arabic labels, enum fallbacks, numeric formatting, configured timezone with Baghdad fallback, and date handling are used in patient-facing presentation. | Medical and record presentation unit tests passed. |
| 2 | Glucose and medications | Glucose values retain their clinical units/precision; medication doses and administration status use localized labels and units. | Record presentation and API contract tests passed. |
| 3 | Daily Journal and BMI | All seven journal categories are present: meals, activity, sleep, symptoms, daily factors, weight, and labs. Labels and dates are localized. Weight shows its unit; BMI uses profile height; Health Profile does not duplicate the journal's weight history. | Journal and Health Profile tests passed. |
| 4 | Dashboard parity | Dashboard presents latest glucose/context, latest medication administration, today's summary, recent glucose, today's activities, chart axes, and event-associated averages. | Dashboard presentation tests passed. |
| 5 | Dashboard Quick Add | Glucose, medication administration, meal, and activity use their create destinations. Success returns to Dashboard and refreshes; cancellation makes no create request; request IDs prevent stale edit reuse; duplicate submits are gated. | Quick Add flow tests passed. |
| 6 | Active sessions | Safe device labels, formatted creation/activity times, explicit individual/revoke-all confirmations, authenticated API calls, and signed-out handling are present. No current-session identity is guessed or sensitive session ID rendered. | Session presentation and Auth API contract tests passed. |

The source audit found no patient-facing raw JSON property names, raw ISO date strings, literal `null`/`undefined`, or known enum codes without a safe localized fallback in the audited presentation paths. Unknown fields use a generic Arabic label; sensitive identifiers and internal fields are filtered.

## Release metadata decision

The live `https://download.techda.dev/updates.json` manifest reported package `dev.techda.sahatisukari`, version `0.2.7`, versionCode `9`, and SHA-256 `2420a2924bfa1b6b8b9abc715c0a72ca5f88a45428c3b49031aef139ff20f449`. The local existing APK matched that manifest hash and its signer matched the previously trusted production certificate. The next version is therefore **0.2.8 / versionCode 10**. This is a patch increment and uses the next code after the verified published release.

The production manifest and deployed `dist/android/SahatiSukari.apk` were left unchanged. The live manifest was read-only checked; nothing was uploaded.

## Release candidate

- Version: `0.2.8` (`versionCode 10`)
- Package: `dev.techda.sahatisukari`
- Minimum SDK: 26
- `debuggable`: false
- APK: `release-candidates/SahatiSukari-0.2.8-rc.apk`
- Size: 15,417,836 bytes
- APK SHA-256: `4a4111d75997e5ff30dabe73dfd6b0397879f5db93c153bab524f6cb7fa70089`
- Signing certificate SHA-256: `601de488e805b8e73c824534fc7c1df063838dc0e810c57a7aae1a93f41c6250`
- Signature: verified; one RSA-3072 signer, APK Signature Scheme v2. Fingerprint matches the previously trusted release certificate.
- Signing material: read from the existing owner-only local signing configuration and keystore; neither was modified or copied into the repository.
- API base URL: production HTTPS endpoint remains `https://sahatisukarie.up.railway.app/api/v1/`.
- The release DEX contains the production API base and existing `download.techda.dev` update-check endpoint; automatic update behavior was not changed.

## Automated checks

Executed from the isolated snapshot containing the audited source plus the version increment:

- `testDebugUnitTest`: PASS — 72 tests, 13 suites, 0 failures/errors/skips.
- `lintDebug`: PASS — 37 non-fatal warnings.
- `assembleDebug`: PASS.
- `lintRelease`: PASS — 37 non-fatal warnings.
- `assembleRelease`: PASS — signed APK produced and verified.

Warnings are dependency-version recommendations and existing lint suggestions (including modifier parameter order, monochrome launcher icon, package visibility query, KTX URI helpers, and version-catalog recommendations). No lint errors were reported. Java 17 was used. The release signing certificate was verified against the trusted fingerprint before the candidate was retained.

No emulator, connected instrumentation test, or phone installation was performed.

## Session flow and limitations

The current session cannot be identified from the backend session list. If the current app session is individually revoked, the follow-up authenticated list refresh is rejected; the existing authenticator rejects the revoked refresh token and clears local tokens, and the Settings state routes through the existing signed-out path. Revoke-all explicitly warns that the current app and website sessions end. No live session was revoked in this audit.

## Existing unrelated files preserved

The following pre-existing files were not staged or overwritten:

- `.codebase-memory/artifact.json` and `.codebase-memory/graph.db.zst`
- `docs/ANDROID_PDF_VERIFICATION.md`
- `dist/android/SahatiSukari.apk`, `dist/android/updates.json`, `dist/android/SHA256SUMS.txt`, and `dist/android/index.html`
- Existing release build output under `app/build/outputs/apk/release/`

The new candidate APK is also deliberately excluded from the commit.

## Physical phone verification checklist

1. Keep the installed app and its data in place; do not uninstall or clear storage.
2. Copy `release-candidates/SahatiSukari-0.2.8-rc.apk` to the phone over USB or another owner-controlled local transfer. Do not upload it to the update host.
3. Confirm the phone's app is `dev.techda.sahatisukari` and is versionCode 9 or lower, then install the candidate through Android's package installer. If Android reports a signature conflict or downgrade, cancel; do not uninstall the existing app.
4. Launch and check sign-in/session restoration, Dashboard, glucose, medication, Daily Journal, Health Profile, Reports, Settings, Arabic RTL, and larger system font scale.
5. Use a designated synthetic account for any save/Quick Add checks. Verify create/cancel behavior without entering real patient data. Do not revoke real sessions or share reports externally.
6. Confirm the app reports version `0.2.8` and that existing app data remains available.

## Non-destructive rollback

Before installation, the currently installed release remains untouched; if the candidate is not installed, continue using it. If `0.2.8` has been installed and a defect is found, do not uninstall or clear app data and do not try to downgrade to the older APK. Prepare a corrective build signed by the same trusted key with a versionCode greater than `10` (for example, a `0.2.9` recovery build), verify its signature, and install it as an in-place update. The candidate has not been published, so production users and the hosted update manifest remain on their current release.

## Git

Version metadata and this audit report are the only Phase 7 files intended for commit. The release APK, signing files, manifest, generated artifacts, and unrelated working-tree changes remain uncommitted.
