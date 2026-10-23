package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordPresentationTest {
    @Test
    fun glucoseCardShowsOrderedArabicClinicalFieldsAndBaghdadTime() {
        val record = Json.parseToJsonElement(
            """{
                "id":"internal-id","patientId":"patient-id","glucoseValue":"104.35","glucoseUnit":"MG_DL",
                "standardizedValueMgdl":"104.350","measurementContext":"AFTER_BREAKFAST",
                "measurementDatetime":"2026-10-23T10:34:36.345Z","source":"FINGERSTICK_METER",
                "minutesAfterMeal":45,"symptomsText":"دوخة بعد الطعام","notes":"قياس منزلي","createdAt":"2026-10-23T10:35:00Z"
            }""".trimIndent(),
        ).jsonObject

        val fields = recordPresentationFields("glucose", record, "Asia/Baghdad", "LATIN")
        assertEquals("قراءة السكر", recordTitle("glucose", record))
        assertEquals("104.35 mg/dL", recordHeadlineValue("glucose", record))
        assertEquals(
            listOf("سياق القياس", "التاريخ والوقت", "مصدر القياس", "الدقائق بعد الوجبة", "الأعراض", "ملاحظات"),
            fields.map { it.label },
        )
        assertEquals("بعد الإفطار", fields[0].value)
        assertEquals("23/10/2026 – 1:34 مساءً", fields[1].value)
        assertEquals("جهاز قياس من الإصبع", fields[2].value)
        assertEquals("دوخة بعد الطعام", fields[4].value)
        assertTrue(fields.none { it.label in setOf("glucoseValue", "glucoseUnit", "measurementContext", "source", "symptomsText") })
        assertTrue(fields.none { it.value.contains("internal-id") || it.value.contains("patient-id") || it.value.contains("104.350") })
    }

    @Test
    fun glucoseCardDoesNotInventMissingContextAndUsesConfiguredTimezone() {
        val record = Json.parseToJsonElement(
            """{"glucoseValue":"8.2","glucoseUnit":"MMOL_L","measurementDatetime":"2026-10-23T10:34:36Z","source":"OTHER"}""",
        ).jsonObject

        val fields = recordPresentationFields("glucose", record, "America/New_York", "ARABIC_INDIC")
        assertFalse(fields.any { it.label == "سياق القياس" })
        assertEquals("٨٫٢ mmol/L", recordHeadlineValue("glucose", record, "ARABIC_INDIC")?.replace('.', '٫'))
        assertEquals("٢٣/١٠/٢٠٢٦ – ٦:٣٤ صباحًا", fields.first { it.label == "التاريخ والوقت" }.value)
        assertEquals("مصدر آخر", fields.first { it.label == "مصدر القياس" }.value)
        assertFalse(fields.any { it.value.contains("8.2") || it.label.contains("قيمة السكر") })
    }

    @Test
    fun glucoseProductionAliasesKeepRawPrecisionAndDoNotRenderGenericApiExtras() {
        val record = Json.parseToJsonElement(
            """{"value":"104.3570","unit":"MG_DL","standardizedValueMgdl":"104.3570","displayValue":"104 mg/dL","measurementDatetime":"2026-10-23T17:10:00+00:00","createdAt":"2026-10-23T17:10:01Z","id":"synthetic-id","backendFutureField":"secret"}""",
        ).jsonObject
        assertEquals("104.357 mg/dL", recordHeadlineValue("glucose", record))
        val fields = recordPresentationFields("glucose", record, "Asia/Baghdad")
        assertEquals("23/10/2026 – 8:10 مساءً", fields.single().value)
        assertEquals(listOf("التاريخ والوقت"), fields.map { it.label })
        assertFalse(fields.any { it.value.contains("2026-") || it.label == "معلومات إضافية" })
    }

    @Test
    fun medicationCardShowsArabicFieldsAndKeepsNameAndDoseUnits() {
        val record = Json.parseToJsonElement(
            """{
                "id":"internal-id","patientId":"patient-id","medicationName":"Metformin XR 500 mg",
                "category":"BIGUANIDE","status":"ACTIVE","strength":"500.000","strengthUnit":"MG",
                "prescribedDose":"1.000","doseUnit":"TABLET","route":"ORAL","frequency":"TWICE_DAILY",
                "usualTime":"08:30:00","startDate":"2026-10-01","endDate":"2026-10-20",
                "prescriberName":"Dr. Ali","notes":"بعد الطعام","createdAt":"2026-10-01T00:00:00Z"
            }""".trimIndent(),
        ).jsonObject

        val fields = recordPresentationFields("medications", record, "Asia/Baghdad", "LATIN")
        assertEquals(
            listOf("فئة الدواء", "الحالة", "تركيز الدواء", "الجرعة الموصوفة", "طريقة الاستخدام", "التكرار", "الوقت المعتاد", "تاريخ البدء", "تاريخ الانتهاء", "اسم الواصف", "ملاحظات"),
            fields.map { it.label },
        )
        assertEquals("بيغوانيد", fields[0].value)
        assertEquals("نشط", fields[1].value)
        assertEquals("500 mg", fields[2].value)
        assertEquals("1 قرص", fields[3].value)
        assertEquals("عن طريق الفم", fields[4].value)
        assertEquals("مرتين يوميًا", fields[5].value)
        assertEquals("8:30 صباحًا", fields[6].value)
        assertEquals("Metformin XR 500 mg", record["medicationName"]?.toString()?.trim('"'))
        assertTrue(fields.none { it.label in setOf("status", "category", "route", "frequency", "strengthUnit", "doseUnit") })
        assertFalse(fields.any { it.value.contains("internal-id") || it.value.contains("patient-id") })
    }

    @Test
    fun administrationHistoryIsSeparateAndNamesTheMedicationWhenReturned() {
        val record = Json.parseToJsonElement(
            """{
                "administration":{"actualDose":"2.000","doseUnit":"MG","administrationDatetime":"2026-10-23T10:00:00Z","status":"MISSED","notes":"تعذّر التناول"},
                "medication":{"medicationName":"Insulin glargine"}
            }""".trimIndent(),
        ).jsonObject

        val fields = recordPresentationFields("administrations", record, "Asia/Baghdad")
        assertEquals("الدواء", fields.first().label)
        assertEquals("Insulin glargine", fields.first().value)
        assertEquals("2 mg", fields.first { it.label == "الجرعة المسجلة" }.value)
        assertEquals("23/10/2026 – 1:00 مساءً", fields.first { it.label == "تاريخ ووقت الجرعة" }.value)
        assertEquals("فاتت", fields.first { it.label == "الحالة" }.value)
    }

    @Test
    fun glucoseEditingRestoresTimestampAndSymptomsWithoutChangingPayloadFieldNames() {
        val record = Json.parseToJsonElement(
            """{"measurementDatetime":"2026-10-23T10:34:36Z","symptomsText":"دوخة","measurementContext":"FASTING","glucoseUnit":"MG_DL"}""",
        ).jsonObject
        assertEquals("2026-10-23", recordFormInitialValue("measurementDate", record, "2099-01-01", "Asia/Baghdad"))
        assertEquals("13:34", recordFormInitialValue("measurementTime", record, "00:00", "Asia/Baghdad"))
        assertEquals("دوخة", recordFormInitialValue("symptoms", record, ""))
        assertEquals("FASTING", recordFormInitialValue("measurementContext", record, "OTHER"))
        assertEquals("MG_DL", recordFormInitialValue("glucoseUnit", record, "MMOL_L"))
        assertEquals("", recordFormInitialValue("measurementTime", Json.parseToJsonElement("""{"measurementDatetime":"invalid"}""").jsonObject, "12:00"))
    }

    @Test
    fun glucoseAndMedicationNumericInputsRejectMalformedDecimalsWithoutChangingBlankOptionalFields() {
        assertTrue(isValidMedicalNumeric("104.35"))
        assertTrue(isValidMedicalNumeric(""))
        assertTrue(isValidMedicalNumeric(null))
        assertFalse(isValidMedicalNumeric("."))
        assertFalse(isValidMedicalNumeric("1.2.3"))
        assertFalse(isValidMedicalNumeric("١٢٣"))
    }
}
