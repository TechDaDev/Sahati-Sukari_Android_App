# Android update system

The app checks `https://download.techda.dev/updates.json` after an authenticated session starts and when the user selects **التحقق من وجود تحديث** in Settings. Startup checks run asynchronously and do not block login. A dismissed optional update is suppressed for that version. Users can check again manually. No periodic WorkManager job runs because updates are optional and startup/manual checks are available.

The manifest parser requires the exact application ID, positive version codes, a nonempty version name, a 64-character SHA-256 value, and the fixed HTTPS URL `https://download.techda.dev/SahatiSukari.apk`. Redirects are disabled. The installed version is compared numerically with `versionCode`. The repository keeps last successful metadata in memory and serves it during connectivity failures. Failed requests use a 60-second retry backoff.

An available update opens the trusted URL in an Android browser. Android does not install it silently. The manifest checksum is informational in this browser-download flow; the app does not download or independently verify the APK. Android package-signature continuity still applies during install.

Manifest shape:

```json
{
  "applicationId": "dev.techda.sahatisukari",
  "versionCode": 2,
  "versionName": "0.2.0",
  "minimumSupportedVersionCode": 1,
  "apkUrl": "https://download.techda.dev/SahatiSukari.apk",
  "sha256": "64 lowercase hexadecimal characters",
  "releaseNotes": "ملاحظات الإصدار باللغة العربية"
}
```

Do not publish a manifest until its APK is signed with the established production key, uploaded, and verified publicly. Do not treat a locally generated unsigned APK or its hash as a production update.
