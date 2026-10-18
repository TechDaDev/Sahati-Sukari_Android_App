# Android security

## Implemented

- HTTPS-only API; cleartext traffic disabled.
- Access and refresh tokens encrypted together with AES-GCM using a non-exportable Android Keystore key. DataStore receives ciphertext only.
- Android cloud backup and device-transfer backup disabled. Passwords remain in transient form state and request bodies only.
- No HTTP logging interceptor or credential logging.
- Single-flight refresh with a fresh `X-Refresh-Request-ID`; failed refresh behavior does not log token values.
- OkHttp transport retries disabled to reduce duplicate mutation risk.
- Report PDFs are stored in app-private cache. A non-exported `FileProvider` grants read-only URIs only to explicit open/share intents.
- Emergency UI uses `ACTION_DIAL`, not direct call permission.

## Limits

Debug APK uses the debug signing key and is not release-ready. No production credentials are embedded. Authenticated live API and account-session behavior still need testing against an authorized synthetic account. Review server-side validation for every API change before shipping.
