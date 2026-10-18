# UI parity checklist

Reference: supplied Samsung Galaxy A55-style dashboard screenshot at 360 × 800 dp. Browser chrome and device-emulation frame are excluded from the app reference.

| Check | Status |
|---|---|
| Native Compose UI; no WebView | Implemented |
| Arabic RTL root layout and Cairo font with OFL license | Implemented |
| Blue/teal palette, white cards, and light background | Implemented; screenshot comparison pending |
| Header, six RTL bottom destinations, emergency action | Implemented; login/emergency UI test passed |
| Dashboard populated from `/api/v1/dashboard` with empty/error states | Implemented; authenticated content not verified |
| Profile, glucose, medications, journal, reports, settings | Native screens and API integration implemented; authenticated live behavior not verified |
| Match dashboard composition against supplied screenshot | NOT VERIFIED: emulator screenshot capture returns a black framebuffer |
| 360 × 800 dp emulator runtime | Tested; API 35 AVD with 720 × 1600 px at 320 dpi |
| Smaller and larger viewport visual review | NOT VERIFIED |

The AVD UI hierarchy confirmed Arabic login content and the emergency action, but that does not establish visual parity. Repeat screenshot capture on a working graphical emulator and compare dashboard, scrolling, and smaller/larger viewports before claiming parity.
