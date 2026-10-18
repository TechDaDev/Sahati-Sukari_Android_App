# Android security

Implemented:

- HTTPS API base; cleartext traffic disabled.
- Access and refresh tokens encrypted together with AES-GCM under a non-exportable Android Keystore key.
- Ciphertext stored in DataStore; Android backup and device-transfer backups disabled.
- Password exists only in the login form state and request body. It is not persisted.
- OkHttp logging interceptor is not installed. Tokens are not written to logs.
- Refresh request uses a UUID request ID and a synchronized single-flight authenticator.
- Emergency action uses `ACTION_DIAL`; app does not place calls automatically.

Remaining review: verify refresh error/replay behavior with MockWebServer, ensure login/session lifecycle handling is complete, and audit every future network logger and crash reporter before release. Current debug app is not release-ready.
