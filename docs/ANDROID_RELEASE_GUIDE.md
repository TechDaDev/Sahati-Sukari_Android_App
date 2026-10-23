# Android release guide

## Signing

Use the established production keystore. Never generate a replacement key for an existing app. Keep the keystore and passwords outside Git and back them up through approved secure storage. A first production key needs owner approval and a secure backup before release.

Provide these Gradle properties or environment variables to sign `release`:

- `ANDROID_RELEASE_STORE_FILE`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

The build remains unsigned when any value is missing. Never use debug signing for production. Verify certificate continuity against the previously released APK before publishing.

## Build and verify

Use JDK 17. Run the release checks requested by the project:

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

Inspect release metadata and signing with Android SDK tools:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

For a signed release, copy the verified APK to `dist/android/SahatiSukari.apk` and create `SHA256SUMS.txt` from that exact file. The in-app updater reads only APK metadata and contents from the fixed APK URL; it does not use an update manifest.

## Hosting

The Namecheap connection available in this environment exposes DNS operations only. It has no file listing, document-root, hosting, or upload operations. Use Namecheap hosting/cPanel or another owner-authorized upload mechanism manually after the owner approves release publication. Inspect existing files before replacement.

For future updates, increment `versionCode`, build with the preserved production key, and upload only the signed APK to `https://download.techda.dev/SahatiSukari.apk`. Verify its HTTP response, package metadata, signer, size, and SHA-256. Keep the previous signed APK as a backup.
