# API integration

Base URL: `https://sahatisukarie.up.railway.app/api/v1/`.

## Authentication

- Login/register, activation OTP, resend activation, forgot-password OTP, reset completion, logout, logout-all, active sessions, and password change use `/auth/*` endpoints.
- Access and refresh tokens are AES-GCM encrypted under an Android Keystore key. Refresh sends a new UUID in `X-Refresh-Request-ID` and is single-flight.
- The session OkHttp client disables connection-failure retries. Authenticator retry happens only after a rejected bearer token; record creation is never retried for a transport error.
- The app accepts patient-role sessions only.

## Implemented patient operations

| Feature | API |
|---|---|
| Dashboard | `GET /dashboard` |
| Profile | `GET/PATCH /profile`; `GET/PUT /profile/diabetes`; `GET/PUT /profile/conditions`; `PUT /profile/reproductive`; `PUT /profile/additional` |
| Glucose | `GET/POST /glucose`; `PATCH/DELETE /glucose/{id}` |
| Medications | `GET/POST /medications`; `PATCH/DELETE /medications/{id}`; `GET/POST /administrations`; `DELETE /administrations/{id}` |
| Journal | `GET/POST` and supported `DELETE` for meals, activities, sleep, symptoms, factors, weight, and labs |
| Reports | `GET /reports/summary`; authenticated `GET /reports/pdf` |
| Settings | `PATCH /settings/contact`; `GET /auth/sessions`; `DELETE /auth/sessions/{id}`; password change and logout-all |

Patient ownership comes from the bearer session. The Android app does not send patient IDs to choose records.

## Contract handling

`PatientModels.kt` defines Kotlin request DTOs for glucose, medications, administrations, meals, activities, sleep, symptoms, factors, weight, labs, and profile sections. `PatientRequestCodec` encodes forms through those models. Fields/enums match `src/server/validation/schemas.ts` in the backend, including lab `testType`, glucose units and context, and profile condition list replacement semantics.

The current OpenAPI document has generic request schemas. Backend source and `docs/ANDROID_API_INTEGRATION.md` are the contract references. Errors show the server Arabic `error` field when available; otherwise the app maps HTTP status to Arabic messages. Timeout configuration is centralized. Automatic offline write replay is disabled.

Lists request 50 records, preserve `sort=desc`, and pass `nextCursor` with the same query filters. Reports accept `from` and `to` dates and are limited to 366 days by backend validation. PDFs stay in app-private cache and are shared through a non-exported `FileProvider` only after a user action.

## Verification limits

MockWebServer tests cover login, registration, refresh header, password recovery request, glucose creation, cursor query, and delete paths. No live authenticated patient request was made. A synthetic account was not provided for end-to-end verification. No backend or production data changes were made.
