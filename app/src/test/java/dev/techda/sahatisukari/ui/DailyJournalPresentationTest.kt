package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyJournalPresentationTest {
    @Test
    fun mealCardShowsTypeDescriptionCarbsLocalDateAndNotes() {
        val record = Json.parseToJsonElement(
            """{"mealType":"DINNER","description":"شوربة عدس with bread","carbohydrateGrams":"30.500","mealDate":"2026-10-23","mealTime":"19:05:00","notes":"وجبة منزلية","id":"synthetic-id"}""",
        ).jsonObject
        val fields = recordPresentationFields("meals", record, "Asia/Baghdad", "LATIN")
        assertEquals(listOf("نوع الوجبة", "الوصف", "الكربوهيدرات", "تاريخ الوجبة", "وقت الوجبة", "ملاحظات"), fields.map { it.label })
        assertEquals("العشاء", fields[0].value)
        assertEquals("شوربة عدس with bread", fields[1].value)
        assertEquals("30.5 g", fields[2].value)
        assertEquals("23/10/2026", fields[3].value)
        assertEquals("7:05 مساءً", fields[4].value)
        assertEquals("وجبة منزلية", fields[5].value)
        assertFalse(fields.any { it.label == "mealType" || it.label == "carbohydrateGrams" })
    }

    @Test
    fun activityAndSleepCardsKeepExactMinutesAndTranslateQuality() {
        val activity = Json.parseToJsonElement(
            """{"activityType":"Walking","durationMinutes":42,"intensity":"MODERATE","activityDate":"2026-10-23","activityTime":"08:15:00","notes":"مشي صباحي"}""",
        ).jsonObject
        val activityFields = recordPresentationFields("activities", activity, "Asia/Baghdad")
        assertEquals("42 دقيقة", activityFields.first { it.label == "المدة" }.value)
        assertEquals("متوسط", activityFields.first { it.label == "الشدة" }.value)
        assertEquals("23/10/2026", activityFields.first { it.label == "تاريخ النشاط" }.value)
        assertEquals("8:15 صباحًا", activityFields.first { it.label == "وقت النشاط" }.value)
        assertEquals("مشي صباحي", activityFields.last().value)

        val sleep = Json.parseToJsonElement(
            """{"durationMinutes":385,"quality":"POOR","sleepDate":"2026-10-22","notes":"استيقاظ مرة واحدة"}""",
        ).jsonObject
        val sleepFields = recordPresentationFields("sleep", sleep, "Asia/Baghdad")
        assertEquals("6 ساعات و 25 دقيقة", sleepFields[0].value)
        assertEquals("ضعيف", sleepFields[1].value)
        assertEquals("22/10/2026", sleepFields[2].value)
        assertEquals("استيقاظ مرة واحدة", sleepFields[3].value)
    }

    @Test
    fun symptomAndFactorCardsTranslateEnumsAndRetainEnteredDescriptions() {
        val symptom = Json.parseToJsonElement(
            """{"symptomType":"EXCESSIVE_THIRST","severity":"MODERATE","symptomDate":"2026-10-23","symptomTime":"14:30:00","notes":"بعد الظهر"}""",
        ).jsonObject
        val symptoms = recordPresentationFields("symptoms", symptom, "Asia/Baghdad")
        assertEquals("عطش شديد", symptoms[0].value)
        assertEquals("متوسط", symptoms[1].value)
        assertEquals("23/10/2026", symptoms.first { it.label == "تاريخ العَرَض" }.value)
        assertEquals("2:30 مساءً", symptoms.first { it.label == "وقت العَرَض" }.value)

        val factor = Json.parseToJsonElement(
            """{"factorType":"STRESS","description":"ضغط العمل","factorDate":"2026-10-23","factorTime":"10:00:00"}""",
        ).jsonObject
        val factors = recordPresentationFields("factors", factor, "Asia/Baghdad")
        assertEquals("توتر نفسي", factors[0].value)
        assertEquals("ضغط العمل", factors[1].value)
        assertEquals("23/10/2026", factors.first { it.label == "تاريخ العامل" }.value)
        assertEquals("10:00 صباحًا", factors.first { it.label == "وقت العامل" }.value)
    }

    @Test
    fun labCardsLocalizeTypesAndCombineBloodPressure() {
        val hba1c = Json.parseToJsonElement(
            """{"testType":"HBA1C","value":"6.200","unit":"%","testDate":"2026-10-01","referenceRange":"4.0-5.6%","notes":"مختبر"}""",
        ).jsonObject
        val labFields = recordPresentationFields("labs", hba1c, "Asia/Baghdad")
        assertEquals("السكر التراكمي (HbA1c)", labFields.first { it.label == "نوع التحليل" }.value)
        assertEquals("6.2", labFields.first { it.label == "النتيجة" }.value)
        assertEquals("%", labFields.first { it.label == "الوحدة" }.value)
        assertEquals("01/10/2026", labFields.first { it.label == "تاريخ التحليل" }.value)
        assertEquals("4.0-5.6%", labFields.first { it.label == "المدى المرجعي" }.value)

        val pressure = Json.parseToJsonElement(
            """{"testType":"BLOOD_PRESSURE","value":"120","valueSecondary":"80","unit":"mmHg","testDate":"2026-10-01"}""",
        ).jsonObject
        val pressureFields = recordPresentationFields("labs", pressure)
        assertEquals("120/80 mmHg", pressureFields.first { it.label == "النتيجة" }.value)
        assertFalse(pressureFields.any { it.label == "قيمة إضافية" || it.label == "الوحدة" })
    }

    @Test
    fun weightCardShowsUnitAndDisplayOnlyBmiWithoutRepeatingWeight() {
        val record = Json.parseToJsonElement(
            """{"weightKg":"75.360","measurementDate":"2026-10-20","notes":"قياس صباحي","id":"synthetic-id"}""",
        ).jsonObject
        assertEquals("75.36 كغم", recordTitle("weight", record))
        val fields = recordPresentationFields("weight", record, patientHeightCm = "175")
        assertEquals("مؤشر كتلة الجسم (BMI)" to "24.6 kg/m²", fields.first().label to fields.first().value)
        assertEquals(listOf("مؤشر كتلة الجسم (BMI)", "تاريخ القياس", "ملاحظات"), fields.map { it.label })
        assertFalse(fields.any { it.value.contains("75.36") || it.label == "الوزن" })
        assertFalse(record.containsKey("bmi"))
    }

    @Test
    fun bmiRejectsMissingInvalidOrNonpositiveMeasurements() {
        assertEquals("24.6 kg/m²", MedicalPresentation.calculateBmi("75.36", "175"))
        assertEquals("٢٤٫٦ kg/m²", MedicalPresentation.calculateBmi("75.36", "175", "ARABIC_INDIC").replace('.', '٫'))
        assertEquals("غير متاح", MedicalPresentation.calculateBmi("75", null))
        assertEquals("غير متاح", MedicalPresentation.calculateBmi("75", "0"))
        assertEquals("غير متاح", MedicalPresentation.calculateBmi("75", "-170"))
        assertEquals("غير متاح", MedicalPresentation.calculateBmi("invalid", "170"))
        assertEquals("غير متاح", MedicalPresentation.calculateBmi("0", "170"))
    }

    @Test
    fun healthProfileKeepsLabsHistoryButNoLongerOffersDuplicateWeightHistory() {
        assertEquals(listOf("labs"), healthProfileHistorySections().map { it.endpoint })
        assertTrue(healthProfileHistorySections().any { it.title == "التحاليل المخبرية" })
        assertFalse(healthProfileHistorySections().any { it.endpoint == "weight" })
    }

    @Test
    fun dailyJournalCardsHideRawPropertyNamesAndProtectUnknownEnums() {
        val record = Json.parseToJsonElement(
            """{"activityType":"Cycling","durationMinutes":90,"intensity":"FUTURE_LEVEL","activityDate":"2026-10-23","activityTime":"08:15:00","recordedAt":"2026-10-23T10:00:00Z","createdAt":"2026-10-23T10:00:00Z","patientId":"synthetic-patient-id"}""",
        ).jsonObject
        val fields = recordPresentationFields("activities", record, "Asia/Baghdad")
        assertEquals("ساعة و 30 دقيقة", fields.first { it.label == "المدة" }.value)
        assertEquals("قيمة محفوظة غير معروفة", fields.first { it.label == "الشدة" }.value)
        val patientFacing = fields.joinToString(" ") { "${it.label} ${it.value}" }
        listOf("activityType", "durationMinutes", "activityDate", "activityTime", "recordedAt", "createdAt", "patientId", "FUTURE_LEVEL").forEach {
            assertFalse("raw value leaked: $it", patientFacing.contains(it))
        }
    }
}
