# Android production release report

**Date:** 2026-10-22
**Branch at start:** `main`
**Starting commit:** `e5a4ac76a6490b3956bb1f231734f0edd94cf34a`

## Release state

| Item | Result |
|---|---|
| Application ID | `dev.techda.sahatisukari` |
| Version name / code | `0.2.0` / `2` |
| Android SDK | min API 26 (Android 8); target API 36; emulator API 35 (Android 15) |
| Release signing | Blocked: no approved production keystore is available. Release configuration supports external secret inputs and emits unsigned APK otherwise. |
| Local APK | `dist/android/SahatiSukari.apk`; 14,908,196 bytes; SHA-256 `2e46d65b36092539f3e7cfb3aa918780f7e30a5613ddb3949b60a27def460f6b`. This is unsigned staging output; do not install as production or upload. |
| Local manifest / checksum | `dist/android/updates.json`, `dist/android/SHA256SUMS.txt`; checksum describes unsigned staging APK only. Regenerate after approved signing. |
| Release debuggable | No; release variant uses default `debuggable=false`. |
| Network / backup | Cleartext disabled; Android backup disabled. Release APK contains configured HTTPS API base URL; no API key embedded. |
| Production APK publication | Not performed. A signed artifact, complete release checks, UI evidence, and owner approval are prerequisites. |
| Update manifest publication | Not performed. Public endpoint currently returns HTTP 404. |
| Namecheap MCP | DNS read/write tools available. No hosting, file-list, document-root, or upload tool is exposed. |
| Hosting destination | DNS points `download.techda.dev` to `162.213.251.220`. HTTP root serves a LiteSpeed directory index; server filesystem path and account access remain unverified. |
| Existing download site | Root returns a LiteSpeed directory listing containing unrelated `PMDAP.apk`. No hosting files were changed. Inspect all existing files before adding `index.html`; local Arabic draft is at `dist/android/index.html`. |
| Public APK URL | `https://download.techda.dev/SahatiSukari.apk` returned HTTP 404 on 2026-10-22. |
| Public manifest URL | `https://download.techda.dev/updates.json` returned HTTP 404 on 2026-10-22. |
| Emulator visual review | Arabic login screenshot reviewed at 360 × 800 dp with SwiftShader: `docs/screenshots/emulator-login.png`. Authenticated dashboard/profile and other navigation screens were not captured. |
| Patient profile parity | Native profile now includes typed fields, diabetes, conditions, reproductive, additional fields, preferences, paged weight/lab history and matching add forms. Synthetic tests cover all profile history fields. Full visual parity remains unverified. |
| Physical-device verification | Not performed. |
| GitHub | Secret-pattern scan passed; implementation pushed to `main` as `f7c2005`. Existing user changes are excluded. |

Automated build and test outcomes are in `ANDROID_TEST_REPORT.md`. Final unit, lint, debug/release build, and connected gates passed in separate runs. One combined invocation hit an Android Lint race while release Hilt sources were generated; isolated lint passed. Do not use this report to claim production readiness, hosting success, or full visual parity.

## Current direct APK updater (2026-10-23)

The update-system implementation now uses the hosted APK itself to identify and validate releases. The historical manifest findings above describe the 2026-10-22 state; they are superseded by the direct APK updater. `updates.json` is neither generated nor required.

- APK source: `dist/android/SahatiSukari.apk`
- Destination: `https://download.techda.dev/SahatiSukari.apk`
- The owner uploads the signed APK manually. No hosting upload was performed by this task.
- The app uses ETag, Last-Modified, size, and content type only as cache hints; it downloads candidate APKs to private cache and validates package ID, greater versionCode, minSdk compatibility, debuggable flag, and signing identity before offering installation.

Keep prior signed releases backed up and preserve the production signing key.
