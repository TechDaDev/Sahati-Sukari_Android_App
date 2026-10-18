# صحتي وسكري Android

Native Android patient application, built with Kotlin and Jetpack Compose. The app does not use WebView.

## Current implementation

The repository now contains the Android project foundation, Cairo Arabic font, RTL Compose theme, login, encrypted session storage, single-flight access-token refresh, authenticated dashboard request, and emergency dial screen.

Registration, email activation, password recovery, profile editing, glucose CRUD, medication workflows, journal records, reports, settings, and most tab screens remain incomplete. Do not use this build for patient care.

## Environment

- Application ID: `dev.techda.sahatisukari`
- Minimum SDK: 26
- Compile/target SDK: 36
- JDK: 17
- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Kotlin: 2.2.21
- Compose BOM: 2025.12.00
- API base: `https://sahatisukarie.up.railway.app/api/v1/`

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK uses debug signing and is for development only.
