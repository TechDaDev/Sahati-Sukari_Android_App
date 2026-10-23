# Dashboard parity — Phase 4

Scope: Android dashboard parity using the existing authenticated `GET /api/v1/dashboard` response. Backend/API, authentication, database, PDF, update flow, and release signing were not changed. Quick Add remains Phase 5 work.

The website source of truth is `src/app/(patient)/dashboard/page.tsx`, `src/server/services/dashboard.ts`, `src/lib/glucose-analysis.ts`, and `src/components/charts.tsx` in the website repository. Android reuses the dashboard response already fetched once by `DashboardViewModel`; optional sections read from that response and do not issue duplicate requests.

| Website feature | Android implementation | Data source | Status | Tests / known limitations |
|---|---|---|---|---|
| Welcome and current date | Arabic dashboard title, patient name, local today date | `profile`, timezone and numeral system from dashboard response | Implemented | Existing date formatter; visual layout remains device review |
| Profile completion | Completion percentage and profile navigation | `dashboard.completion` | Partial parity | Android does not list the website's individual missing profile fields |
| Conditional latest-glucose information note | Mirrors website wording at `<60` or `>400 mg/dL`; identifies this as descriptive entry confirmation, not diagnosis | Latest reading `standardizedValueMgdl` | Implemented | Boundary/invalid-value unit test; same thresholds and note as website |
| Latest glucose | Preferred-unit value, translated context, configured-timezone date/time; value/unit use content-aware LTR text | `dashboard.latestGlucose` | Implemented | Tests for mg/dL and mmol/L, context, Baghdad conversion, missing and malformed values |
| Today's reading count | Localized count and empty state | `dashboard.todayReadings` | Implemented | Existing dashboard field |
| Latest medication administration | Actual status, medication name, actual dose/unit when present, local date/time; does not fall back to prescribed dose | `dashboard.lastAdministration` | Implemented | Tests cover MISSED/DELAYED, actual dose, and no prescribed-dose substitution |
| Today's activity total | Total recorded minutes and count | `dashboard.activityMinutes`, `dashboard.todayActivities` | Implemented | Existing duration formatter |
| Fourteen-day glucose trend | Value axis with unit, padded data-derived scale and ticks; date/time ticks; points sorted chronologically and spaced by timestamp | `dashboard.trendData` | Implemented | Tests cover ordering, irregular gaps, outlier scale, empty/single/two points, month boundary, units. No therapeutic target bands. Canvas graph has an accessibility summary and no point-selection tooltip. |
| Today's summary | RTL label/value rows for meals, symptoms, latest sleep, latest weight, and latest HbA1c | `mealsToday`, `todaySymptoms`, `lastSleep`, `latestWeight`, `latestHba1c` | Implemented | Existing formatters preserve meaningful precision and remove trailing zeros; tests include 8.100% → 8.1% |
| Average glucose differences around recorded events | Shows up to three medication events and two meals; compares recorded-reading averages in the three hours before and after each event | `trendData`, `recentAdministrations`, `recentMeals` | Implemented | Kotlin reproduces website interval bounds; insufficient windows show the website's Arabic empty message. Copy explicitly says this is descriptive and does not establish causation. |
| Recent glucose readings | Latest six with preferred unit, context, and local date/time; link opens Glucose tab | `dashboard.recentGlucose` | Implemented | Uses service's chronological response ordering; missing context is identified as unrecorded |
| Today's activities | Type, duration, intensity, and local date/time; link opens Daily Journal on Activities | `dashboard.todayActivities` | Implemented | Tests cover translated intensity, localized time, and malformed optional records |
| Report navigation and safety footer | Existing Reports navigation and Arabic data/safety disclaimer retained | App navigation and static copy | Partial parity | Android footer remains shorter than the website's expanded medication and classification notes |
| Website Quick Add | Not implemented in this phase | Website-only QuickActions | Intentionally out of scope | Phase 5 |

## Calculation and unit notes

- `getPatientDashboard` returns `trendData` in the patient's preferred unit and chronological order. Android uses these values as-is for the chart and event averages; it converts the latest reading from `standardizedValueMgdl` only for display. The trend is sorted by parsed instant again defensively.
- The website's `windowAverageDelta` uses a 3-hour window before the event (`>= start`, `< event`) and after it (`> event`, `<= end`). Android uses the same bounds. A zero or positive/negative difference is descriptive only.
- Chart scale is derived from observed values plus padding. It does not add clinical thresholds or discard outliers. X coordinates use elapsed time so uneven measurement intervals remain uneven.
- Website/API unit: `MG_DL` displays `mg/dL`; `MMOL_L` displays `mmol/L`. Missing or malformed optional records are omitted or shown with an Arabic empty state rather than raw JSON keys or timestamps.

## Verification

- `./gradlew testDebugUnitTest`: passed (55 tests).
- `./gradlew lintDebug`: passed.
- `./gradlew assembleDebug`: passed.
- No emulator or connected tests were run, as required for Phase 4. Review chart tick readability, Arabic wrapping, and font scaling on a physical Android phone before release.
- No production APK was generated.
