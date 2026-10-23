package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthProfilePresentationTest {
    @Test fun mapsProfileFieldsAndConditionsToArabicAllowListedContent() {
        val source = Json.parseToJsonElement(
            """{
              "id":"synthetic-uuid-9876",
              "patientId":"synthetic-patient-uuid",
              "fullName":"مريض تجريبي",
              "gender":"FEMALE",
              "dateOfBirth":"1990-01-02",
              "age":36,
              "heightCm":165,
              "nationalIdMasked":"••••1234",
              "email":"patient@example.test",
              "emailVerified":false,
              "phoneNumber":"+9647000000000",
              "phoneVerified":false,
              "preferredGlucoseUnit":"MG_DL",
              "numeralSystem":"ARABIC_INDIC",
              "completion":{"percent":68,"missing":["privateDatabaseField"]},
              "diabetes":{"diabetesType":"TYPE_2","diagnosisDate":null,"monitoringMethod":"CGM","notes":null,"patientId":"hidden"},
              "conditions":[
                {"id":"condition-uuid","patientId":"patient-uuid","conditionType":"HYPERTENSION","status":"NOT_DIAGNOSED","updatedAt":"2026-10-18T00:00:00Z","notes":null},
                {"conditionType":"FUTURE_CONDITION","status":"FUTURE_STATUS"}
              ],
              "reproductive":null,
              "allergies":null,
              "smokingStatus":"FORMER",
              "familyHistoryDiabetes":"UNSURE",
              "healthNotes":"معلومة صحية تجريبية"
            }""".trimIndent(),
        ).jsonObject

        val model = healthProfilePresentation(source)
        val visible = buildList {
            addAll(model.summary.flatMap { listOf(it.label, it.value) })
            addAll(model.identity.flatMap { listOf(it.label, it.value) })
            addAll(model.diabetes.flatMap { listOf(it.label, it.value) })
            addAll(model.conditions.flatMap { listOf(it.label, it.status, it.notes.orEmpty()) })
            addAll(model.additional.flatMap { listOf(it.label, it.value) })
            addAll(model.preferences.flatMap { listOf(it.label, it.value) })
        }.joinToString(" ")

        assertEquals(68, model.completionPercent)
        assertTrue(model.summary.any { it.label == "الجنس" && it.value == "أنثى" })
        assertTrue(model.summary.any { it.label == "تاريخ الميلاد" && it.value == "٠٢/٠١/١٩٩٠" })
        assertTrue(model.diabetes.any { it.value == "النوع الثاني" })
        assertEquals("ارتفاع ضغط الدم", model.conditions.first().label)
        assertEquals("غير مُشخّصة", model.conditions.first().status)
        assertEquals("حالة طبية غير معروفة", model.conditions[1].label)
        assertEquals("حالة محفوظة غير معروفة", model.conditions[1].status)
        assertTrue(model.preferences.any { it.value == "mg/dL" })
        assertFalse(visible.contains("synthetic-uuid"))
        assertFalse(visible.contains("patientId"))
        assertFalse(visible.contains("conditionType"))
        assertFalse(visible.contains("FUTURE_STATUS"))
        assertFalse(visible.contains("privateDatabaseField"))
        assertFalse(visible.contains("null"))
    }

    @Test fun unknownProfileEnumsUseSafeLocalizedFallback() {
        assertEquals("قيمة محفوظة غير معروفة", profileEnumLabel("diabetesType", "UNRECOGNIZED_TYPE"))
        assertEquals("حالة طبية غير معروفة", conditionTypeLabel("UNRECOGNIZED_CONDITION"))
        assertEquals("حالة محفوظة غير معروفة", conditionStatusLabel("UNRECOGNIZED_STATUS"))
    }

    @Test fun conditionReplacementBodyKeepsAllTypesAndExistingNotes() {
        val body = buildConditionsBody(
            mapOf(
                "status:HYPERTENSION" to "NOT_DIAGNOSED",
                "notes:HYPERTENSION" to "ملاحظة تجريبية",
                "status:SERVER_ADDED_CONDITION" to "UNKNOWN",
            ),
        )
        val conditions = body["conditions"]!!.jsonArray.map { it.jsonObject }
        val hypertension = conditions.first { it["conditionType"]?.jsonPrimitive?.content == "HYPERTENSION" }
        assertEquals(9, conditions.size)
        assertEquals("NOT_DIAGNOSED", hypertension["status"]?.jsonPrimitive?.content)
        assertEquals("ملاحظة تجريبية", hypertension["notes"]?.jsonPrimitive?.content)
        assertTrue(conditions.any { it["conditionType"]?.jsonPrimitive?.content == "SERVER_ADDED_CONDITION" })
        assertTrue(conditions.all { it["status"]?.jsonPrimitive?.content?.isNotBlank() == true })
    }

    @Test fun nullProfileProducesNoPatientFields() {
        val model = healthProfilePresentation(null)
        assertFalse(model.hasVisibleContent)
        assertTrue(model.conditions.isEmpty())
    }
}
