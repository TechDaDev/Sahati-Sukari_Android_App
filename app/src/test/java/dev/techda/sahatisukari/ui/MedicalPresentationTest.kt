package dev.techda.sahatisukari.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalPresentationTest {
    @Test
    fun everyPatientFacingBackendEnumValueHasAnExplicitMapping() {
        val backendCodes = mapOf(
            MedicalEnumField.GLUCOSE_UNIT to setOf("MG_DL", "MMOL_L"),
            MedicalEnumField.MEASUREMENT_CONTEXT to setOf("FASTING", "BEFORE_BREAKFAST", "AFTER_BREAKFAST", "BEFORE_LUNCH", "AFTER_LUNCH", "BEFORE_DINNER", "AFTER_DINNER", "BEFORE_SLEEP", "OTHER"),
            MedicalEnumField.GLUCOSE_SOURCE to setOf("FINGERSTICK_METER", "CGM", "LAB", "OTHER", "NOT_SPECIFIED"),
            MedicalEnumField.MEDICATION_CATEGORY to setOf("INSULIN", "BIGUANIDE", "SULFONYLUREA", "DPP4_INHIBITOR", "SGLT2_INHIBITOR", "GLP1_AGONIST", "TZD", "MEGLITINIDE", "ALPHA_GLUCOSIDASE_INHIBITOR", "ANTIHYPERTENSIVE", "STATIN", "OTHER"),
            MedicalEnumField.DOSE_UNIT to setOf("UNIT", "MG", "MCG", "G", "ML", "TABLET", "CAPSULE", "DROP", "PUFF", "SACHET"),
            MedicalEnumField.ADMINISTRATION_ROUTE to setOf("SUBCUTANEOUS", "INTRAVENOUS", "INTRAMUSCULAR", "ORAL", "INHALED", "TOPICAL", "OTHER"),
            MedicalEnumField.MEDICATION_FREQUENCY to setOf("ONCE_DAILY", "TWICE_DAILY", "THREE_TIMES_DAILY", "FOUR_TIMES_DAILY", "WITH_MEALS", "BEFORE_MEALS", "AT_BEDTIME", "AS_NEEDED", "WEEKLY", "OTHER"),
            MedicalEnumField.MEDICATION_STATUS to setOf("ACTIVE", "INACTIVE"),
            MedicalEnumField.ADMINISTRATION_STATUS to setOf("TAKEN", "MISSED", "DELAYED"),
            MedicalEnumField.MEAL_TYPE to setOf("BREAKFAST", "LUNCH", "DINNER", "SNACK", "OTHER"),
            MedicalEnumField.ACTIVITY_INTENSITY to setOf("LIGHT", "MODERATE", "VIGOROUS"),
            MedicalEnumField.SLEEP_QUALITY to setOf("GOOD", "FAIR", "POOR", "NOT_SPECIFIED"),
            MedicalEnumField.SYMPTOM_TYPE to setOf("DIZZINESS", "SWEATING", "TREMOR", "EXCESSIVE_THIRST", "FATIGUE", "HEADACHE", "BLURRED_VISION", "PALPITATIONS", "NAUSEA", "OTHER"),
            MedicalEnumField.SYMPTOM_SEVERITY to setOf("MILD", "MODERATE", "SEVERE"),
            MedicalEnumField.DAILY_FACTOR_TYPE to setOf("ILLNESS", "STRESS", "TRAVEL", "MENSTRUATION", "OTHER"),
            MedicalEnumField.LAB_TEST_TYPE to setOf("HBA1C", "CREATININE", "EGFR", "URINE_ACR", "BLOOD_PRESSURE", "TOTAL_CHOLESTEROL", "LDL", "HDL", "TRIGLYCERIDES", "OTHER"),
            MedicalEnumField.GENDER to setOf("MALE", "FEMALE"),
            MedicalEnumField.DIABETES_TYPE to setOf("TYPE_1", "TYPE_2", "OTHER", "UNKNOWN"),
            MedicalEnumField.MONITORING_METHOD to setOf("FINGERSTICK", "CGM", "BOTH", "NOT_SPECIFIED"),
            MedicalEnumField.YES_NO_UNSURE to setOf("YES", "NO", "UNSURE"),
            MedicalEnumField.HYPOGLYCEMIA_AWARENESS to setOf("NORMAL", "REDUCED", "UNSURE", "NOT_SPECIFIED"),
            MedicalEnumField.TREATMENT_APPROACH to setOf("INSULIN", "NON_INSULIN_MEDICATION", "BOTH", "LIFESTYLE_ONLY", "NOT_SPECIFIED"),
            MedicalEnumField.CONDITION_TYPE to setOf("HYPERTENSION", "HEART_DISEASE", "KIDNEY_DISEASE", "HIGH_CHOLESTEROL", "RETINOPATHY", "NEUROPATHY", "FOOT_PROBLEMS", "OTHER"),
            MedicalEnumField.CONDITION_STATUS to setOf("DIAGNOSED", "NOT_DIAGNOSED", "UNKNOWN"),
            MedicalEnumField.SMOKING_STATUS to setOf("NEVER", "CURRENT", "FORMER", "NOT_SPECIFIED"),
            MedicalEnumField.PREGNANCY_STATUS to setOf("NOT_APPLICABLE", "NOT_PREGNANT", "PREGNANT", "BREASTFEEDING", "NOT_SPECIFIED"),
            MedicalEnumField.NUMERAL_SYSTEM to setOf("LATIN", "ARABIC_INDIC"),
        )

        assertEquals(MedicalEnumField.entries.toSet(), backendCodes.keys)
        backendCodes.forEach { (field, codes) ->
            assertEquals("backend enum drift in $field", codes, MedicalPresentation.knownCodes(field))
            codes.forEach { code ->
                val translation = MedicalPresentation.label(field, code)
                assertNotEquals("missing $field.$code", MedicalPresentation.UNKNOWN_ENUM, translation)
                assertNotEquals("raw enum leaked: $field.$code", code, translation)
                if (field !in setOf(MedicalEnumField.GLUCOSE_UNIT, MedicalEnumField.DOSE_UNIT)) {
                    assertTrue("translation should be readable Arabic: $field.$code", translation.any { it in '\u0600'..'\u06ff' })
                }
            }
        }
    }

    @Test
    fun overlappingCodesUseTheirMedicalContextAndUnknownsStaySafe() {
        assertEquals("سياق آخر", MedicalPresentation.label(MedicalEnumField.MEASUREMENT_CONTEXT, "OTHER"))
        assertEquals("عَرَض آخر", MedicalPresentation.label(MedicalEnumField.SYMPTOM_TYPE, "OTHER"))
        assertEquals("حالات طبية أخرى", MedicalPresentation.label(MedicalEnumField.CONDITION_TYPE, "OTHER"))
        assertEquals(MedicalPresentation.UNKNOWN_ENUM, MedicalPresentation.label(MedicalEnumField.MEAL_TYPE, "NEW_BACKEND_VALUE"))
        assertEquals("غير متوفر", MedicalPresentation.label(MedicalEnumField.GLUCOSE_UNIT, null))
        assertEquals("حالة طبية غير معروفة", profileMedicalEnumLabel("conditionType", "NEW_CONDITION"))
        assertEquals("حالة محفوظة غير معروفة", profileMedicalEnumLabel("conditionStatus", "NEW_STATUS"))
    }

    @Test
    fun numericFormattersKeepFieldSpecificPrecisionAndUnits() {
        assertEquals("126 mg/dL", MedicalPresentation.formatGlucose("126.0", "MG_DL"))
        assertEquals("8.1 mmol/L", MedicalPresentation.formatGlucose("8.10", "MMOL_L"))
        assertEquals("8.1 mmol/L", MedicalPresentation.formatGlucoseFromMgDl("146", "MMOL_L"))
        assertEquals("75.36 كغم", MedicalPresentation.formatWeight("75.360"))
        assertEquals("8.1%", MedicalPresentation.formatHbA1c("8.100"))
        assertEquals("25 kg/m²", MedicalPresentation.formatBmi("25.00"))
        assertEquals("30.5 g", MedicalPresentation.formatCarbohydrates("30.500"))
        assertEquals("75.36", MedicalPresentation.formatLabValue("75.360"))
        assertEquals("1.5 mg", MedicalPresentation.formatMedicationDose("1.500", "MG"))
        assertEquals("120/80 mmHg", MedicalPresentation.formatBloodPressure("120.0", "80.00"))
        assertEquals("ساعة و 30 دقيقة", MedicalPresentation.formatActivityDuration("90"))
        assertEquals("7 ساعات", MedicalPresentation.formatSleepDuration("420"))
        assertEquals("٧٥٫٣٦", MedicalPresentation.formatDecimal("75.360", 2, "ARABIC_INDIC").replace('.', '٫'))
    }

    @Test
    fun datesDistinguishLocalCalendarDatesFromTimestampInstants() {
        assertEquals("23/10/2026", MedicalPresentation.formatDateOnly("2026-10-23", "LATIN"))
        assertEquals("23/10/2026 – 1:34 مساءً", MedicalPresentation.formatInstant("2026-10-23T10:34:36.345Z", "Asia/Baghdad", "LATIN"))
        assertEquals("23/10/2026 – 6:34 صباحًا", MedicalPresentation.formatInstant("2026-10-23T10:34:36.345Z", "America/New_York", "LATIN"))
        assertEquals("23/10/2026 – 1:34 مساءً", MedicalPresentation.formatInstant("2026-10-23T10:34:36.345Z", "Not/AZone", "LATIN"))
        assertEquals(MedicalPresentation.UNAVAILABLE_DATE, MedicalPresentation.formatInstant("2026-10-23TbadZ"))
        assertEquals(MedicalPresentation.UNAVAILABLE_DATE, MedicalPresentation.formatDateOnly("2026-10-23T10:00:00Z"))
        assertEquals("1:34 مساءً", MedicalPresentation.formatTimeOnly("13:34:00", "LATIN"))
        assertEquals("وقت غير متاح", MedicalPresentation.formatTimeOnly("not-a-time"))
    }

    @Test
    fun recordPresentationHidesKeysAndIdentifiersButRetainsUnknownFreeText() {
        assertNull(MedicalPresentation.presentRecordField("patientId", "private-id"))
        assertNull(MedicalPresentation.presentRecordField("createdAt", "2026-10-23T10:00:00Z"))
        val futureEnum = MedicalPresentation.presentRecordField("severity", "FUTURE_SEVERITY")
        assertNotNull(futureEnum)
        assertEquals("قيمة محفوظة غير معروفة", futureEnum?.value)
        val futureFreeText = MedicalPresentation.presentRecordField("futureClinicalField", "ملاحظة سريرية محفوظة")
        assertEquals("معلومات إضافية", futureFreeText?.label)
        assertEquals("ملاحظة سريرية محفوظة", futureFreeText?.value)
        val date = MedicalPresentation.presentRecordField("measurementDatetime", "2026-10-23T10:34:36.345Z")
        assertEquals("التاريخ والوقت", date?.label)
        assertEquals("23/10/2026 – 1:34 مساءً", date?.value)
    }

    @Test
    fun medicalValueStyleUsesContentDirectionForMixedArabicAndLatin() {
        assertEquals(TextDirection.ContentOrLtr, medicalValueTextStyle(TextStyle.Default).textDirection)
        assertTrue(MedicalPresentation.formatGlucose("8.1", "MMOL_L").contains("mmol/L"))
        assertTrue(MedicalPresentation.label(MedicalEnumField.LAB_TEST_TYPE, "HBA1C").contains("HbA1c"))
    }
}
