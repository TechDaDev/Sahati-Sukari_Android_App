# Android test report

- `assembleDebug`: passed before the latest refresh error-handling/test-dependency edits. APK exists at `app/build/outputs/apk/debug/app-debug.apk`; rebuild required to ensure it matches current source.
- `testDebugUnitTest`: attempted; Gradle failed while writing global transform caches because the host filesystem reached 100% usage. The added MockWebServer test did not complete.
- `lintDebug`: not completed; the combined Gradle invocation stopped at the cache/resource failure.
- Emulator/UI tests: blocked. AVD `m30_ops_api35` reported insufficient disk space; host had approximately 133 MB free at launch.
- Live authenticated API tests: not run. No synthetic account credentials were provided through an approved secure channel.

Do not treat API, UI, security, or visual parity gates as passed.
