package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthHistoryPresentationTest {
    @Test fun weightHistoryShowsEveryPatientFieldAndHidesIds() {
        val data = Json.parseToJsonElement("""{"items":[{"id":"internal","patientId":"internal","weightKg":"72.5","measurementDate":"2026-10-20","notes":"متابعة"}]}""").jsonObject
        val row = healthHistoryPresentation("weight", data).single()
        assertTrue(row.fields.any { it.label == "الوزن" && it.value.contains("72.5") })
        assertTrue(row.fields.any { it.label == "تاريخ القياس" })
        assertTrue(row.fields.any { it.label == "ملاحظات" && it.value == "متابعة" })
        assertFalse(row.fields.any { it.value == "internal" })
    }

    @Test fun labHistoryShowsAllWebsiteFieldsAndLocalizesTestType() {
        val data = Json.parseToJsonElement("""{"items":[{"id":"internal","patientId":"internal","testType":"HBA1C","value":"6.2","valueSecondary":"6.1","unit":"%","testDate":"2026-10-20","referenceRange":"4-5.6","notes":"متابعة"}]}""").jsonObject
        val row = healthHistoryPresentation("labs", data).single()
        assertEquals("السكر التراكمي (HbA1c)", row.title)
        assertEquals(7, row.fields.size)
        assertTrue(row.fields.any { it.label == "قيمة إضافية" })
        assertTrue(row.fields.any { it.label == "المدى المرجعي" })
        assertFalse(row.fields.any { it.value == "internal" })
    }
}
