# Android architecture

## Stack

- Kotlin, Jetpack Compose, Material 3, Navigation Compose dependency
- MVVM with `StateFlow`
- Hilt dependency injection
- Retrofit and OkHttp
- Kotlin Serialization
- DataStore Preferences with AES-GCM encryption using an Android Keystore key

## Current structure

`MainActivity` applies RTL and app theme. `AuthViewModel` handles session restoration and login. `AuthRepository` calls login and current-user endpoints. `TokenStore` encrypts access and refresh tokens together before writing ciphertext to DataStore. `SessionAuthenticator` serializes refresh attempts and sends `X-Refresh-Request-ID`. `DashboardViewModel` loads the authenticated dashboard. Compose UI includes login, a partial dashboard, bottom navigation, and emergency assistance.

## Remaining work

Feature repositories and ViewModels for profile, glucose, medications, journal, reports, and settings are not implemented. Several visible destinations still show an incomplete state. Navigation state preservation, refresh UI, and app-wide Arabic error mapping need completion.
