package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSummaryPresentationTest {
    @Test
    fun rendersKnownArabicLabelsAndNeverExposesApiKeysOrIdentifiers() {
        val summary = Json.parseToJsonElement(
            """{
              "range":{"from":"2026-10-01T00:00:00.000Z","to":"2026-10-22T00:00:00.000Z"},
              "preferences":{"unit":"MG_DL","numeralSystem":"LATIN"},
              "glucose":{"count":3,"averageMgdl":125.5,"averageDisplay":"١٢٥ mg/dL","minMgdl":90,"minDisplay":"٩٠ mg/dL","maxMgdl":180,"maxDisplay":"١٨٠ mg/dL"},
              "adherence":{"taken":4,"missed":1,"delayed":0},
              "activities":{"count":2,"totalMinutes":40},
              "meals":{"count":1,"totalCarbs":30},
              "sleep":{"count":1,"averageMinutes":420},
              "patientId":"private-id","internalFlag":true
            }""".trimIndent(),
        ).jsonObject

        val presentation = presentReportSummary(summary)
        val labelsAndValues = presentation.rows.joinToString(" ") { "${it.first} ${it.second}" }

        assertFalse(presentation.isEmpty)
        assertTrue(labelsAndValues.contains("عدد قراءات السكر"))
        assertTrue(labelsAndValues.contains("جرعات فائتة"))
        assertTrue(labelsAndValues.contains("mg/dL"))
        assertFalse(labelsAndValues.contains("patientId"))
        assertFalse(labelsAndValues.contains("private-id"))
        assertFalse(labelsAndValues.contains("internalFlag"))
    }
}
