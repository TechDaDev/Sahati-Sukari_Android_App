# Sahati Wa Sukari Authenticated Android UI Audit

**Date:** 2026-10-22
**Overall status:** PASS WITH LIMITATIONS
**Repository branch:** `main`

## Scope and privacy

- Authenticated with the owner-designated test account through the native Android login form. Credentials are not reproduced here.
- No patient record was created, edited, or deleted. Forms were opened and left unsaved; the delete dialog was not confirmed. No real emergency call was placed.
- `docs/credential.md` is excluded by `.gitignore`, remains untracked, and has local mode `0600`.
- Screenshots in `docs/screenshots/authenticated/` mask account details, record values, identifiers, and chart data. Login screenshot has empty fields.

## Emulator and build

- AVD: `sahati_api35_visible`, x86_64, Android 15 / API 35.
- Primary configuration: 720 × 1600 px at 320 dpi = 360 × 800 dp.
- Responsive checks: 600 × 1334 px (300 × 667 dp) and 900 × 2000 px (450 × 1000 dp), each at Android font scale 1.3. All six bottom destinations remained visible. Device size and font scale were restored afterward.
- App version: 0.2.0 (versionCode 2).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
  Size: 22,099,652 bytes
  SHA-256: `4ce1f3a92cb84383f42f95406588a2b7cae0c0e11ab1d52c45debc80d92dde67`
- APK was installed for testing. No APK was published.

## Page results

| Screen | Load and API | RTL and field visibility | Functionality and defects | Website visual parity |
|---|---|---|---|---|
| Login and authentication | **PASS.** Native login completed; login returned HTTP 201 and authenticated data reads returned HTTP 200. Patient navigation appeared after sign-in. | Arabic login page and emergency entry visible. | Session tokens are encrypted with Android Keystore AES-GCM in the existing `TokenStore`; the authenticated app survived a process restart. | Not compared; see owner review request below. |
| Dashboard | **PASS.** Dashboard loaded; startup requests returned HTTP 200. | Arabic header, summary cards, quick actions, and trend-chart area rendered. Private values are masked in the screenshot. | No crash or user-visible error observed. Dashboard content scrolls. | Not compared; no authenticated web reference was available. |
| Health Profile | **PASS WITH LIMITATIONS.** Profile and history requests returned HTTP 200. | Personal, diabetes, conditions, reproductive, additional, and preference editor categories were reachable; the chip row requires horizontal scrolling. Weight and laboratory history sections rendered their empty states and add controls. | Editor and weight form were opened without saving. History lists had no visible records to edit or delete. | Not compared; sanitized screenshots are provided. |
| Glucose | **PASS WITH LIMITATIONS.** Glucose list returned HTTP 200. | Arabic page and add-measurement form rendered. Form includes value, unit, date, time, context, source, symptoms, and notes. | Empty required value kept the form save button disabled. No record edit/delete actions were visible, so editing and deletion confirmation could not be exercised. No measurement was submitted. | Not compared. |
| Medications | **PASS WITH LIMITATIONS.** Medication and administration reads returned HTTP 200. | Arabic list, medication form, and dose-record form opened. | Medication form save remained disabled while required name was empty. No medication edit/delete actions were visible. Dose form was inspected but not saved. | Not compared. |
| Daily Journal | **PASS WITH LIMITATIONS.** Meals, activity, sleep, symptoms, daily factors, weight, and laboratory categories were reachable by horizontal scrolling; category reads returned HTTP 200. | Arabic category labels and meal form rendered. | Meal form opened with default required values; save was not pressed. Other category forms and data changes were not submitted. | Not compared. |
| Reports | **BLOCKED IN PART.** Report summary returned HTTP 200 and rendered a summary or empty state. PDF request returned HTTP 503 on two attempts; no PDF file was cached. | Arabic date filters, summary, and report controls rendered. Report values are masked. | PDF viewer and Android share chooser could not be tested because the PDF download failed. No file was shared. | Not compared. |
| Settings | **PASS WITH LIMITATIONS.** Account profile and active-session reads returned HTTP 200; active sessions were listed. | Arabic account, glucose-unit and numeral preferences, password controls, session controls, installed-version information, and update button are available. | Preferences and security controls were inspected without changing them. Manual update check showed a connection error. Public HEAD checks returned HTTP 404 for both `https://download.techda.dev/updates.json` and `https://download.techda.dev/SahatiSukari.apk`. Logout was not activated. | Not compared. |
| Emergency Help | **PASS.** Page loaded without an API dependency. | Arabic instructions, the unified Iraq emergency number **911**, call button, and medical disclaimer were visible. | Call button was not activated. | Not compared. |

## Confirmed correction

The app sets `LayoutDirection.Rtl`, but patient-facing text used `TextAlign.End`, which placed Arabic headings and copy on the physical left. I changed those alignments to RTL start alignment, added a shared page-title composable, and added the `pageTitleAlignsToRightInRtlLayout` rendering regression test. The corrected headings render on the right; the six bottom-navigation icons and labels remain centered.

The debug HTTP status logger records only method and status code. It does not record URL, headers, or response bodies. The final log scan found no test credential values, Authorization headers, bearer/JWT patterns, crash markers, or 403 responses. One PDF request produced `POST 200` followed by `GET 503`; based on `SessionAuthenticator`'s code path, the POST is an automatic refresh after an initial 401. That initial 401 is inferred because the current application-level logger records the final response after authentication retry.

## Test results

| Check | Result |
|---|---|
| `./gradlew testDebugUnitTest` | **PASS**, 16 tests, 0 failures |
| `./gradlew lintDebug` | **PASS** |
| `./gradlew assembleDebug` | **PASS** |
| `./gradlew connectedDebugAndroidTest` | **PASS**, 6 tests, 0 failures |

The first connected run encountered the existing signed-in emulator state and failed the login-screen precondition. Gradle's clean connected run then passed all six tests.

## Screenshots

Sanitized screenshots are in [`docs/screenshots/authenticated/`](screenshots/authenticated/), including Dashboard, Health Profile and history, Glucose and form, Medications and form, Daily Journal and meal form, Reports and PDF error, Settings and security/preferences/sessions, Emergency Help, and Login.

## Remaining limitations and owner visual review

- PDF viewing/sharing remains blocked by the report endpoint's HTTP 503 response.
- The update-check host returns HTTP 404 for its manifest and APK.
- The connected browser tools were unavailable, and the web lookup tool could not load the backend URL. Only a non-visual public-root HTTP 200 check was possible. I could not compare authenticated Android screens with the mobile website.
- Please compare the sanitized Dashboard, Health Profile, Glucose, Medications, Daily Journal, Reports, Settings, and Emergency Help screenshots with the mobile website. Website visual parity remains unverified until that review.
- No real test account write or delete was attempted; the live lists did not expose glucose or medication edit/delete controls during this session.

## Git

- Starting commit: `3d478b9d0d7314d43be7a1860fe9a04aeb8cf761`.
- Final audit commit and push to `main`: recorded in the repository history after this report was created.
- Credential file, unsigned release staging artifacts, generated codebase-memory files, and the pre-existing `Theme.kt` edit were excluded from the commit.
