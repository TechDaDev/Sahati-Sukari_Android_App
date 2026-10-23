# Android APK update system

Sahati checks only `https://download.techda.dev/SahatiSukari.apk`. It does not request `updates.json`, open a browser, or use Google Play in-app updates.

## Check and validation flow

- The app checks after startup without gating authentication, when the patient manually checks Settings, and through a unique WorkManager job with a 24-hour interval and network constraint.
- The check uses HTTP `HEAD` where supported and inspects status, ETag, Last-Modified, Content-Length, and Content-Type. A 405/501 response falls back to `GET`. Redirects are disabled.
- ETag, timestamps, and size are cache hints only. A changed or previously unseen APK is downloaded to app-private cache and inspected with Android `PackageManager`. Only a valid, non-debuggable APK for this package with a greater versionCode, a supported minSdk, and the installed signing identity is considered an update.
- Verified metadata is cached against the remote fingerprint. When headers are stable, later checks avoid downloading the APK again. A verified candidate can remain staged in app cache for installation.
- Downloads have a 100 MiB limit, use a temporary `.part` file, report progress, can be cancelled, and remove partial files on failure. The app accepts only the fixed HTTPS host and APK path with normal certificate validation.
- The update prompt shows installed and available versions. **تحديث الآن** opens Android's package installation confirmation. Android may first request the per-app “install unknown apps” permission. After granting it, tapping **تحديث الآن** again uses the cached APK and does not download it again. The app never installs silently or uninstalls itself.
- If no newer version is found, Settings shows **أنت تستخدم أحدث إصدار من التطبيق.** A failed check shows **تعذّر التحقق من وجود تحديثات. يرجى المحاولة لاحقاً.** A periodic WorkManager result is shown in-app on the next launch; it does not post a system notification.

## Publishing

Build each release with the preserved production signing key and increment `versionCode`. Upload only the signed `dist/android/SahatiSukari.apk`, replacing the file at the fixed URL. No JSON or other metadata file is required. Hosting should return a valid APK content type, a successful HEAD response where possible, and updated file metadata after replacement.

The local PMDAP Flutter checkout was inspected read-only. No APK update/download/install implementation was present in its Dart or Android source; only Flutter's normal `versionCode` build configuration was found. Sahati therefore implements this native Kotlin flow directly.

## Limits

Android controls the final install confirmation. WorkManager periodic work is best-effort and runs no more frequently than Android permits; a background-discovered update is presented when Sahati next opens. The release must still be manually uploaded by the owner.
