# Android architecture

## Stack

Kotlin 2.2.21, Jetpack Compose with Material 3, MVVM with `StateFlow`, Hilt, Retrofit, OkHttp, Kotlin Serialization, DataStore, and Android Keystore. App content uses Cairo and RTL layout.

## Structure

- `MainActivity` applies the theme and RTL direction.
- `AuthViewModel`, `AuthRepository`, `AuthApi`, `TokenStore`, and `SessionAuthenticator` implement public authentication and protected sessions.
- `PatientApi` exposes documented patient, record, settings, and report operations. `PatientModels.kt` contains schema-shaped request DTOs and the request codec.
- `DashboardViewModel` loads the dashboard. `PatientSectionViewModel` serves the profile, glucose, medication, and journal tabs, including cursor pagination and record mutations.
- `ReportsViewModel` loads report summaries and stores downloaded PDFs in app-private cache. `SettingsViewModel` handles contact preferences, sessions, password changes, and logout-all.
- `SahatiApp.kt` owns the six-tab shell, dashboard, and emergency page. Feature forms use server-defined fields and enums.

## Navigation and data

Six bottom destinations follow the supplied mobile screenshot. Tabs switch through Compose state. Emergency assistance can open before authentication. The dashboard trend chart uses server `trendData`; the client does not add clinical thresholds or interpretation.

Clinical writes require a live authenticated response. The client does not queue writes for offline replay. Record lists request 50 rows and pass the server's opaque cursor for subsequent pages.

## Build

Use JDK 17, Android SDK 36, and the checked-in Gradle wrapper. See `README.md` for commands. `Navigation Compose` remains available in the dependency catalog, but tab switching currently uses state rather than a navigation graph.
