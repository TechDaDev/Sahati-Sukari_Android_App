# Phase 8 — Android record presentation corrections

## Cause

`recordPresentationFields` appended every unconsumed JSON property. That generic fallback added duplicate aliases and audit metadata, and displayed unknown timestamp keys as generic information. The API's `mealDatetime`, `activityDatetime`, and `symptomDatetime` fields also had no instant-format mapping. Glucose results were repeated in the details instead of being given a distinct headline. Health Profile separately fetched and displayed the laboratory history already available in the Journal.

The backend symptom contract was checked read-only: symptom records contain the symptom type, severity, `symptomDatetime`, notes, and standard identifiers/audit time. The UI now displays all patient-facing clinical fields supported by that contract.

## Changes

- Glucose records now show one prominent value/unit headline and ordered clinical details. Both app and API aliases are accepted, recognized mg/dL and mmol/L units are displayed, and the supplied numeric precision is preserved without repeating the result in details.
- Meal headers combine the localized meal type with the entered description. Activity headers preserve the recorded activity name, symptom headers use the localized symptom name, sleep headers show a useful duration, and weight/lab headers retain their weight/test identity.
- Details now use endpoint-specific allowlists. Known datetime fields are converted to the patient's timezone (Baghdad fallback); legacy local date/time pairs are combined without shifting date-only values. Arbitrary API properties, identifiers, audit timestamps, duplicates, and generic “معلومات إضافية” rows are not shown.
- Journal tabs use the requested Arabic names. The horizontal selector keeps its selected tab visible and remains scrollable for long labels and larger text.
- Health Profile no longer fetches or renders duplicate weight/lab history or exposes its lab-add/pagination actions. Journal laboratory history and the backend/API remain unchanged.

## Verification

- `testDebugUnitTest`: PASS, 73 tests, 0 failures.
- `lintDebug`: PASS.
- `assembleDebug`: PASS.
- `lintRelease`: PASS.
- `assembleRelease`: PASS using the existing trusted release key.
- APK signature: PASS, APK Signature Scheme v2, RSA-3072. Certificate SHA-256: `601de488e805b8e73c824534fc7c1df063838dc0e810c57a7aae1a93f41c6250`.
- APK metadata: `dev.techda.sahatisukari`, versionName `0.2.9`, versionCode `11`, minSdk `26`, targetSdk `36`. Production API base remains HTTPS. Release build type is non-debuggable.
- Candidate: `release-candidates/SahatiSukari-0.2.9-rc-phase8.apk`; 15,417,836 bytes; SHA-256 `a8a3423fbcc83db1f4dfb93ac4f779587c9e3d195dd86b7dcfb5ce89f86c96b4`.
- Existing `0.2.8` candidate was preserved. The new code 11 candidate is upgrade-compatible with code 10. APK was not uploaded and update manifest was not changed.

## Remaining device checks

No emulator or connected instrumentation test was used. The only ADB device visible was `emulator-5554`, which was not interacted with. Therefore the owner's physical-device installed version, screenshot-level RTL appearance, larger-font behavior, and on-device update/install behavior remain unverified. APK signature and package/version metadata were verified locally.
