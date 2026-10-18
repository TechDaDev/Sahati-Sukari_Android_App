# صحتي وسكري Android

Native Android patient app using Kotlin, Jetpack Compose, Material 3, and Arabic RTL. No WebView.

## Implemented

- Login, registration, email activation, password recovery, encrypted token storage, and single-flight refresh.
- Six RTL destinations: dashboard, health profile, glucose, medications, journal, and reports.
- Profile display and edits for supported profile sections.
- Glucose create, edit, list, delete, and trend chart; medication create/edit/delete and administration records.
- Meals, activities, sleep, symptoms, daily factors, weight, and lab records. Lists request 50 records and support cursor pagination.
- Report summary, private PDF download, PDF open/share, account contact/preferences, password change, session management, and logout-all.
- Emergency assistance available before login. `ACTION_DIAL` opens the phone app; it does not place calls.

## Environment

- Application ID: `dev.techda.sahatisukari`
- Minimum SDK: 26; compile/target SDK: 36
- JDK: 17; AGP: 8.13.2; Gradle: 8.13; Kotlin: 2.2.21; Compose BOM: 2025.12.00
- API: `https://sahatisukarie.up.railway.app/api/v1/`

The API request DTOs and form fields follow the current backend validation schemas. The service's OpenAPI document still uses generic request bodies in places. No production patient account was used for verification.

## Build and verification

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It uses debug signing and is for development only. Current backend authenticated flows and visual dashboard parity remain unverified without a synthetic account and a working emulator screenshot surface.

See `docs/` for architecture, API contracts, security, visual checks, and test evidence.
