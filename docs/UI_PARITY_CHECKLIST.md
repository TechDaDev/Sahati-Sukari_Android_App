# UI parity checklist

Reference: supplied Samsung Galaxy A55-style dashboard screenshot at 360 × 800 dp. Browser chrome and device-emulation frame are excluded from the app reference.

| Check | Status |
|---|---|
| Native Compose UI; no WebView | Implemented |
| Arabic RTL root layout and Cairo font with OFL license | Implemented |
| Blue/teal palette, white cards, light background | Implemented; screenshot comparison pending |
| Header and emergency action | Implemented |
| Six RTL bottom destinations with equal widths and centered icon/label | Implemented; Compose tests passed at normal and increased font scale |
| Dashboard populated from `/api/v1/dashboard` with empty/error states | Implemented; authenticated read-only GET returned HTTP 200 |
| Profile shows localized typed sections and condition labels | Implemented; synthetic Compose fixture passed |
| Profile condition editor preserves notes and submits all conditions | Implemented; Compose test passed with synthetic fixture data |
| Weight and lab history in Health Profile | Implemented with pagination and add forms; synthetic tests cover all website fields |
| Glucose, medications, journal, reports, settings screens | Native screens and API integration implemented; not all live sections were exercised |
| Login runtime screenshot | Captured and visually reviewed at `docs/screenshots/emulator-login.png`; API 35 AVD, SwiftShader, 720 × 1600 px at 320 dpi (360 × 800 dp) |
| Authenticated dashboard/profile visual review | NOT VERIFIED: no authorized synthetic login account or comparison screenshots supplied |
| 360 × 800 dp emulator runtime | Tested; API 35 AVD at 720 × 1600 px and 320 dpi; connected Compose tests passed |
| Smaller/larger viewport visual review | NOT VERIFIED |
| Physical-device review | NOT VERIFIED |

Automated tests establish semantics and layout bounds for tested components, not full visual parity. Compare authenticated dashboard and profile screens with supplied references on a device before claiming parity.
