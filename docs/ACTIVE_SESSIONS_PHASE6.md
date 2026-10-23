# Active Sessions — Phase 6

## Scope

This Android-only change improves the Settings screen's **الأجهزة النشطة** section. It does not modify the backend, website, authentication contract, or other Settings features.

## Backend contract discovered

The authenticated mobile API exposes:

- `GET /api/v1/auth/sessions`: active sessions with `id`, `deviceLabel`, `createdAt`, `lastUsedAt`, and `refreshExpiresAt`.
- `DELETE /api/v1/auth/sessions/{id}`: revokes an owned, active session and returns a revocation result.
- `POST /api/v1/auth/logout-all`: revokes the account's mobile sessions and web cookie sessions, including the calling mobile session.

The server filters the list by the authenticated user and excludes already revoked sessions. The Android client uses its existing authenticated `AuthApi` calls. It refreshes the list after a successful individual revocation; the local list is not changed on a failed revoke or failed refresh.

## Device metadata and current session

The Android client currently registers the generic label `Android`. The session list does not provide a model name, operating system version, browser, IP address, user agent, status label, or reliable marker for the current session. The UI therefore displays **أندرويد** when that exact generic label is present, otherwise **جهاز غير معروف**, and does not mark any row **هذا الجهاز**. Session IDs are retained only for the revoke request and are not rendered.

The backend does not identify which listed session belongs to this app instance. Consequently, the app cannot warn that a specific row is the current session before sending its confirmed revoke request. If the user revokes the current session, the subsequent authenticated list refresh is rejected; the client clears local credentials and returns through the existing signed-out navigation path.

## Presentation and dates

Cards display the device label, the safe fallback description **جلسة تسجيل دخول**, session creation time, and last activity as distinct Arabic-labeled values. They reuse Phase 1 `MedicalPresentation` formatting, honor the patient's timezone, fall back to Asia/Baghdad, and show **تاريخ غير متاح** for missing or invalid dates. Raw ISO strings, internal IDs, and JSON field names are not shown. The cards and dialogs use the existing RTL Material 3 layout.

## Revocation and confirmations

- Individual revoke opens a confirmation dialog describing the selected session using only safe display fields. The DELETE call occurs only after confirmation. A successful DELETE is followed by a fresh list request and a success notice.
- Revoke-all opens a separate warning that sessions on the app and website, including this session, will end. The API call occurs only after explicit confirmation; on success the existing local token cleanup and signed-out navigation run.
- Confirmation cancellation makes no API call. Busy state disables repeated confirm/revoke actions.

## Error handling and security

- Requests continue through the existing authenticated API and token refresh stack; no new authentication mechanism was added.
- HTTP 401/403 failures clear the local token store and set the existing signed-out state. Other failures preserve the current session list and display an Arabic error; network I/O has a localized connectivity message.
- A failed revoke does not remove a session locally. A missing response envelope is treated as an error instead of rendering an indefinite empty/loading state.
- Session identifiers are internal request data only. Tokens and secrets are not displayed or added to logs.
- The server does not expose current-session identity or richer device metadata; improving either requires a separate backend contract change.

## Verification

Synthetic fixtures cover formatted creation/activity timestamps, missing and null metadata, invalid dates/timezones, empty/malformed lists, selected-session confirmation and cancellation, revoke-all confirmation, duplicate-action gating, and authentication/server failure state handling. Auth API contract tests cover the list, individual revoke, and revoke-all HTTP methods and paths, plus a failed revoke response. `testDebugUnitTest` passed all 72 tests (13 suites); `lintDebug` and `assembleDebug` passed. These are local unit/contract checks, not authenticated production session operations or UI instrumentation tests.

Commands and results are recorded in the Phase 6 handoff. No production account was used. No emulator, connected instrumentation test, production APK build, backend change, or production session revocation was performed.

## Remaining checks and limitations

- Current-device identification is unsupported by the present API; the UI intentionally does not guess.
- Device identity is only as specific as the server's generic `Android` label.
- Emulator and physical-device visual checks, including larger font-scale inspection, remain unperformed per the task's no-emulator constraint.
