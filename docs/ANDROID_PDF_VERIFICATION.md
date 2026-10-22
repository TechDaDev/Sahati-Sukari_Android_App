# Android PDF report verification

**Date:** 2026-10-22
**Status:** Android hardening verified locally; production end-to-end verification blocked by backend HTTP 503 diagnosis.

## Implementation reviewed

- Requests use the existing authenticated `PatientApi.reportPdf` call for `GET /api/v1/reports/pdf`; existing Retrofit authentication and refresh handling remain in place.
- Error responses are closed and mapped to Arabic messages. JSON error bodies are never written as PDF files.
- `ReportPdfStorage` writes to the app cache under `reports/health-report.pdf`, checks the PDF signature, terminal marker, and declared content length, and removes partial files on failure.
- The existing non-exported `FileProvider` grants temporary URI read access only to `cache/reports/`.
- Viewing is an explicit user action. Sharing is an explicit user action that launches the Android chooser. Neither action is available after a failed download or when the selected dates differ from the cached report dates.
- Reports summary now uses known Arabic labels and formatted values instead of flattening arbitrary JSON keys. Identifiers and unknown/internal properties are omitted.

## Local verification

- `./gradlew testDebugUnitTest`: PASS (17 tests, 0 failures).
- `./gradlew lintDebug`: PASS.
- `./gradlew assembleDebug`: PASS; local debug APK only, not published.
- `./gradlew connectedDebugAndroidTest`: PASS (6 tests, 0 failures) on `sahati_api35_visible`, Android 15 / API 35. The earlier invocation had one login-screen assertion failure. A direct `pm clear` attempt reported that the package was not installed; the subsequent connected run passed all six tests. The cause of the first assertion failure was not established.
- PDF storage tests cover valid bytes, non-PDF payload, truncated content, and content-length mismatch.
- Summary tests cover Arabic presentation labels, unit/value formatting, and omission of raw identifiers/internal keys.
- `git diff --check`: PASS.

## Not verified

- No new authenticated production PDF request was made. The previously recorded HTTP 503 remains without a confirmed Railway exception, so there is no deployment ID or backend commit to report.
- End-to-end download using the authorized account, opening the PDF in a device reader, and chooser behavior were not exercised in this run. The instrumented suite does not cover those interactions.
- Arabic PDF rendering against a production-generated file remains unverified. Local renderer integration evidence is not a substitute for the production response.
- No patient records were changed and no report was shared.

## Commits

- Android change commit: pending.
- Backend fix commit: none; blocked pending Railway runtime evidence.
- Railway deployment ID: none.
