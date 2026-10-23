package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardPresentationTest {
    @Test
    fun latestGlucoseUsesStandardMgDlPreferredUnitContextAndPatientTimezone() {
        val record = Json.parseToJsonElement(
            """{"glucoseValue":"8.2","glucoseUnit":"MMOL_L","standardizedValueMgdl":"147.8","measurementContext":"AFTER_DINNER","measurementDatetime":"2026-10-23T18:00:00Z"}""",
        ).jsonObject
        val displayed = DashboardPresentation.latestGlucose(record, "MG_DL", "Asia/Baghdad", "LATIN")
        assertEquals("147.8 mg/dL", displayed?.value)
        assertEquals("بعد العشاء", displayed?.context)
        assertEquals("23/10/2026 – 9:00 مساءً", displayed?.dateTime)

        val mmol = DashboardPresentation.latestGlucose(record, "MMOL_L", "Asia/Baghdad", "LATIN")
        assertEquals("8.2 mmol/L", mmol?.value)
    }

    @Test
    fun latestGlucoseDoesNotInventMissingContextOrExposeMalformedTimestamp() {
        val record = Json.parseToJsonElement("""{"standardizedValueMgdl":"bad","measurementDatetime":"not-iso"}""").jsonObject
        val displayed = DashboardPresentation.latestGlucose(record, "MG_DL", null, null)
        assertEquals("—", displayed?.value)
        assertNull(displayed?.context)
        assertNull(displayed?.dateTime)
        assertFalse(displayed.toString().contains("not-iso"))
    }

    @Test
    fun medicationCardUsesActualStatusDoseAndDateNeverPrescribedDose() {
        val record = Json.parseToJsonElement(
            """{"administration":{"status":"MISSED","actualDose":null,"doseUnit":"UNIT","prescribedDose":"12","administrationDatetime":"2026-10-23T18:00:00Z"},"medication":{"medicationName":"إنسولين سريع"}}""",
        ).jsonObject
        val missed = DashboardPresentation.latestAdministration(record, "Asia/Baghdad", "LATIN")
        assertEquals("فاتت", missed?.status)
        assertEquals("إنسولين سريع", missed?.medicationName)
        assertNull(missed?.actualDose)
        assertEquals("23/10/2026 – 9:00 مساءً", missed?.dateTime)

        val delayed = Json.parseToJsonElement("""{"administration":{"status":"DELAYED","actualDose":"3.50","doseUnit":"UNIT"},"medication":{"medicationName":"دواء"}}""").jsonObject
        val delayedDisplay = DashboardPresentation.latestAdministration(delayed, null, null)
        assertEquals("متأخرة", delayedDisplay?.status)
        assertEquals("3.5 U", delayedDisplay?.actualDose)
    }

    @Test
    fun chartPointsSortByInstantKeepOutliersAndMapIrregularTimeIntervals() {
        val elements = Json.parseToJsonElement(
            """[{"at":"2026-11-01T12:00:00Z","value":400},{"at":"2026-10-31T00:00:00Z","value":80},{"at":"2026-11-01T00:00:00Z","value":120},{"at":"bad","value":22},{"at":"2026-11-01T04:00:00Z","value":"NaN"}]""",
        ).jsonArray
        val points = DashboardPresentation.trendPoints(elements)
        assertEquals(listOf(80.0, 120.0, 400.0), points.map { it.value })
        assertEquals(0f, DashboardPresentation.chartTimePosition(points.first(), points))
        assertEquals(1f, DashboardPresentation.chartTimePosition(points.last(), points))
        assertTrue(DashboardPresentation.chartTimePosition(points[1], points) in 0.0f..1.0f)
        val scale = DashboardPresentation.chartScale(points, "MG_DL")
        assertTrue(scale!!.minimum < 80.0)
        assertTrue(scale.maximum > 400.0)
        assertEquals(6, scale.ticks.size)
    }

    @Test
    fun chartHandlesEmptyOneAndTwoPointsAndKeepsMonthBoundaryAxisLabels() {
        assertNull(DashboardPresentation.chartScale(emptyList(), "MG_DL"))
        val empty = DashboardPresentation.trendPoints(null)
        assertTrue(empty.isEmpty())
        val one = DashboardPresentation.trendPoints(Json.parseToJsonElement("""[{"at":"2026-10-31T22:00:00Z","value":100}]""").jsonArray)
        assertEquals(0.5f, DashboardPresentation.chartTimePosition(one.single(), one))
        assertEquals(1, DashboardPresentation.chartAxisLabels(one, "Asia/Baghdad", "LATIN").size)
        val two = DashboardPresentation.trendPoints(Json.parseToJsonElement("""[{"at":"2026-11-01T00:00:00Z","value":100},{"at":"2026-10-31T22:00:00Z","value":101}]""").jsonArray)
        val labels = DashboardPresentation.chartAxisLabels(two, "UTC", "LATIN")
        assertEquals(2, labels.size)
        assertTrue(labels.first().label.contains("31/10"))
        assertTrue(labels.last().label.contains("01/11"))
        assertEquals("mmol/L", DashboardPresentation.glucoseUnitLabel("MMOL_L"))
        assertEquals("mg/dL", DashboardPresentation.glucoseUnitLabel("MG_DL"))
        val clustered = DashboardPresentation.trendPoints(Json.parseToJsonElement("""[
            {"at":"2026-10-31T00:00:00Z","value":90},
            {"at":"2026-10-31T00:01:00Z","value":91},
            {"at":"2026-10-31T00:02:00Z","value":92},
            {"at":"2026-11-02T00:00:00Z","value":93}
        ]""").jsonArray)
        assertEquals(2, DashboardPresentation.chartAxisLabels(clustered, "UTC", "LATIN").size)
    }

    @Test
    fun eventAveragesUseWebsiteThreeHourWindowsAndNeutralMissingState() {
        val points = DashboardPresentation.trendPoints(Json.parseToJsonElement(
            """[{"at":"2026-10-23T06:59:59Z","value":90},{"at":"2026-10-23T07:00:00Z","value":100},{"at":"2026-10-23T10:00:00Z","value":110},{"at":"2026-10-23T13:00:00Z","value":130}]""",
        ).jsonArray)
        val around = DashboardPresentation.windowAverageDelta(points, "2026-10-23T10:00:00Z")
        assertEquals(100.0, around.first!!, 0.0001)
        assertEquals(130.0, around.second!!, 0.0001)
        assertEquals(30.0, around.third!!, 0.0001)
        val insufficient = DashboardPresentation.windowAverageDelta(points, "2026-10-23T06:59:59Z")
        assertNull(insufficient.first)
        assertNull(insufficient.third)
    }

    @Test
    fun dashboardEventRowsTranslateMealNamesAndRetainNeutralNoData() {
        val data = Json.parseToJsonElement(
            """{"trendData":[{"at":"2026-10-23T08:00:00Z","value":90}],"recentAdministrations":[{"administration":{"status":"MISSED","administrationDatetime":"2026-10-23T10:00:00Z"},"medication":{"medicationName":"دواء"}}],"recentMeals":[{"mealType":"DINNER","mealDatetime":"2026-10-23T11:00:00Z"}]}""",
        ).jsonObject
        val events = DashboardPresentation.eventDeltas(data, "Asia/Baghdad", "LATIN")
        assertEquals(listOf("دواء: دواء", "وجبة: العشاء"), events.map { it.label })
        assertTrue(events.all { it.delta == null })
        assertTrue(events.all { it.at.contains("23/10/2026") })
    }

    @Test
    fun recentGlucoseAndActivityCardsLocalizeRecordsWithoutRawIsoValues() {
        val glucose = Json.parseToJsonElement((0..6).joinToString(prefix = "[", postfix = "]") { hour ->
            """{"standardizedValueMgdl":"${100 + hour}","measurementContext":"${if (hour == 6) "AFTER_BREAKFAST" else "FASTING"}","measurementDatetime":"2026-10-23T${hour.toString().padStart(2, '0')}:00:00Z"}"""
        }).jsonArray
        val recent = DashboardPresentation.recentGlucose(glucose, "MG_DL", "Asia/Baghdad", "LATIN")
        assertEquals(6, recent.size)
        assertEquals("106 mg/dL", recent.first().value)
        assertEquals("بعد الإفطار", recent.first().context)
        assertFalse(recent.first().toString().contains("2026-10-23T"))

        val activities = Json.parseToJsonElement(
            """[{"activityType":"مشي","durationMinutes":42,"intensity":"MODERATE","activityDatetime":"2026-10-23T06:00:00Z"},{"activityType":"","durationMinutes":"bad"}]""",
        ).jsonArray
        val rows = DashboardPresentation.todayActivities(activities, "Asia/Baghdad", "LATIN")
        assertEquals(1, rows.size)
        assertEquals("42 دقيقة", rows.single().duration)
        assertEquals("متوسط", rows.single().intensity)
        assertEquals("23/10/2026 – 9:00 صباحًا", rows.single().time)
        assertFalse(rows.toString().contains("activityDatetime"))
    }

    @Test
    fun summaryPrecisionMatchesWebsiteWithoutChangingStoredValues() {
        assertEquals("8.1%", MedicalPresentation.formatHbA1c("8.100"))
        assertEquals("122.5 كغم", MedicalPresentation.formatWeight("122.500"))
        val summary = DashboardPresentation.todaySummary(Json.parseToJsonElement(
            """{"mealsToday":2,"todaySymptoms":[{},{}],"lastSleep":[{"durationMinutes":420}],"latestWeight":[{"weightKg":"122.500"}],"latestHba1c":[{"value":"8.100"}]}""",
        ).jsonObject, "LATIN")
        assertEquals(listOf("الوجبات المسجّلة", "الأعراض المسجّلة", "آخر نوم مسجّل", "الوزن الأخير", "آخر HbA1c مسجّل"), summary.map { it.label })
        assertEquals(listOf("2", "2", "7 ساعات", "122.5 كغم", "8.1%"), summary.map { it.value })
        val partial = DashboardPresentation.todaySummary(Json.parseToJsonElement("""{"latestWeight":[null],"latestHba1c":[{"value":"bad"}],"lastSleep":"wrong"}""").jsonObject, null)
        assertEquals("—", partial[2].value)
        assertEquals("—", partial[3].value)
        assertEquals("—", partial[4].value)
    }

    @Test
    fun conditionalGlucoseNoteMatchesWebsiteEntryConfirmationRules() {
        assertNull(DashboardPresentation.glucoseInformationNote("100", "LATIN"))
        assertTrue(DashboardPresentation.glucoseInformationNote("59.6", "ARABIC_INDIC")!!.contains("أقل من المستويات المعتادة"))
        val high = DashboardPresentation.glucoseInformationNote("401", "LATIN")!!
        assertTrue(high.contains("401 mg/dL"))
        assertTrue(high.contains("اتصل بالطوارئ"))
        assertNull(DashboardPresentation.glucoseInformationNote("not-number", "LATIN"))
    }
}
