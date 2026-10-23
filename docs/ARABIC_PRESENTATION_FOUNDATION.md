# Arabic medical presentation foundation

## Scope

Phase 1 adds shared presentation helpers and applies them to existing profile/history, record-list, dashboard summary, and report-summary paths. It does not change API contracts, models, medical data, authentication, update behavior, or release signing. It is not a full screen-by-screen migration.

## Reused and introduced utilities

- Existing profile/history and report-summary presentation models remain the data boundary for those screens.
- `MedicalPresentation` centralizes Arabic enum labels, numeric/date/time formatting, safe generic record-field presentation, and numeral localization.
- `ArabicFieldLabel`, `MedicalValueText`, `MedicalDateText`, and `MedicalLabelValueRow` provide direction-aware Compose text and label/value layout. Parent RTL direction is preserved; mixed values use content-aware direction rather than forcing the card or screen to LTR.

## Enum coverage

Mappings cover the current backend patient-facing values for glucose units, measurement contexts and sources; medication categories, status, route, frequency, dose units and administration status; meal types, activity intensity, sleep quality, symptom type/severity and daily factors; laboratory test types; and profile gender, diabetes type, monitoring method, history flags, hypo-awareness, treatment approach, conditions/status, smoking, pregnancy and numeral system. The mapping test compares each enum set with the backend schema snapshot used by the project. Unknown enum values resolve to a safe Arabic fallback; they are not displayed as raw codes or guessed descriptions.

## Numeric presentation

- Glucose record values retain their entered unit; standardized report-summary mg/dL values are converted only when the configured display unit is mmol/L.
- Weight allows up to two decimals; HbA1c, BMI, carbohydrates and mmol/L glucose use field-appropriate precision. Laboratory values retain supplied precision while removing insignificant trailing zeros.
- Medication dose, blood pressure, and activity/sleep durations have dedicated formatters. Invalid or absent numeric values display an em dash. No clinical interpretation is performed.
- Numerals follow the patient's configured numeral system where that preference is available. Conventional Latin unit notation is preserved.

## Dates, timezone, and direction

Timestamp instants are parsed with their offset and formatted in the patient's timezone; invalid timezone configuration falls back to `Asia/Baghdad`. Date-only values are parsed as calendar dates and are not shifted across timezones. Invalid or missing values use a safe Arabic unavailable label. Arabic labels follow RTL layout; embedded Latin abbreviations and numeric measurements retain readable content direction.

## Remaining screen-level work

This phase does not migrate every patient-facing screen. Generic record cards now use the safe field presenter, but other screens may still contain direct date or value rendering and should be migrated in a later scoped phase. Unknown free-text fields are retained under a generic Arabic label; their semantic labels need explicit mappings if they become important to patients. Visual typography and actual-device bidi behavior were not inspected because emulator use was explicitly out of scope.

## Verification

Executed for this phase:

- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`

All three passed. Unit coverage includes enum-set parity and unknown values, nulls, numeric precision/units, duration conversion, date-only and timezone handling, invalid timestamps, safe record fields, and Compose text-direction configuration. No emulator, connected instrumented test, or production APK build was run.
