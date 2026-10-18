# API integration

Base URL: `https://sahatisukarie.up.railway.app/api/v1/`.

The live OpenAPI document lists 45 paths. Many request/response objects use generic schemas. Backend source and `docs/ANDROID_API_INTEGRATION.md` in the web repository provide additional contract evidence. Do not infer DTO fields from generic OpenAPI objects.

Implemented calls: `POST /auth/login`, `POST /auth/refresh` through the authenticator, `GET /auth/me`, `POST /auth/logout`, and `GET /dashboard`.

Backend contracts confirmed from source/docs:

- Login body: `email`, `password`, optional `deviceLabel`; successful response includes access/refresh tokens and expiry timestamps.
- Refresh body: `refreshToken`; include UUID `X-Refresh-Request-ID`. Tokens rotate. Keep refresh single-flight.
- Successful JSON uses `{ ok: true, data: ... }`. Errors use `{ ok: false, error, code, fields }`.
- Dashboard includes `profile`, `age`, `bmi`, `diabetes`, `conditions`, `reproductive`, and `dashboard` data.
- Record lists use cursor pagination. Do not queue clinical writes for offline replay without idempotency support.

Remaining: implement all other endpoint repositories using shared backend validation schemas. Verify field names, enums, nullability, and pagination before adding mutation forms. Do not make production backend changes.
