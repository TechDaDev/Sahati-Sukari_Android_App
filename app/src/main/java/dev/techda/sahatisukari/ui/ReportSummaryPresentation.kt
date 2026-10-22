package dev.techda.sahatisukari.ui

import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal data class ReportSummaryPresentation(val rows: List<Pair<String, String>>) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

internal fun presentReportSummary(summary: JsonObject): ReportSummaryPresentation {
    val rows = mutableListOf<Pair<String, String>>()
    val glucose = summary.objectAt("glucose")
    val adherence = summary.objectAt("adherence")
    val activities = summary.objectAt("activities")
    val meals = summary.objectAt("meals")
    val sleep = summary.objectAt("sleep")
    val preferences = summary.objectAt("preferences")
    val latinDigits = preferences.primitiveAt("numeralSystem") == "LATIN"
    val unit = preferences.primitiveAt("unit")

    summary.objectAt("range").let { range ->
        val from = range.primitiveAt("from")?.take(10)
        val to = range.primitiveAt("to")?.take(10)
        if (from != null && to != null) rows += "الفترة" to "${digits(from, latinDigits)} – ${digits(to, latinDigits)}"
    }
    glucose.primitiveAt("count")?.let { rows += "عدد قراءات السكر" to number(it, 0, latinDigits) }
    glucose.displayValue("averageDisplay")?.let { rows += "متوسط السكر" to it }
        ?: glucose.numberAt("averageMgdl")?.let { rows += "متوسط السكر" to glucoseValue(it, unit, latinDigits) }
    glucose.displayValue("minDisplay")?.let { rows += "أدنى قراءة" to it }
        ?: glucose.numberAt("minMgdl")?.let { rows += "أدنى قراءة" to glucoseValue(it, unit, latinDigits) }
    glucose.displayValue("maxDisplay")?.let { rows += "أعلى قراءة" to it }
        ?: glucose.numberAt("maxMgdl")?.let { rows += "أعلى قراءة" to glucoseValue(it, unit, latinDigits) }

    listOf("taken" to "جرعات أُخذت", "missed" to "جرعات فائتة", "delayed" to "جرعات متأخرة")
        .forEach { (key, label) -> adherence.primitiveAt(key)?.let { rows += label to number(it, 0, latinDigits) } }
    activities.primitiveAt("count")?.let { rows += "عدد الأنشطة" to number(it, 0, latinDigits) }
    activities.primitiveAt("totalMinutes")?.let { rows += "مدة النشاط" to "${number(it, 0, latinDigits)} دقيقة" }
    meals.primitiveAt("count")?.let { rows += "عدد الوجبات المسجلة" to number(it, 0, latinDigits) }
    meals.numberAt("totalCarbs")?.let { rows += "إجمالي الكربوهيدرات" to "${number(it.toString(), 1, latinDigits)} غرام" }
    sleep.primitiveAt("count")?.let { rows += "عدد سجلات النوم" to number(it, 0, latinDigits) }
    sleep.numberAt("averageMinutes")?.let { rows += "متوسط مدة النوم" to "${number(it.toString(), 0, latinDigits)} دقيقة" }
    summary.arrayAt("medications")?.let { rows += "الأدوية المسجلة" to number(it.size.toString(), 0, latinDigits) }
    summary.arrayAt("weights")?.let { rows += "سجلات الوزن" to number(it.size.toString(), 0, latinDigits) }
    summary.arrayAt("hba1c")?.let { rows += "فحوصات السكر التراكمي" to number(it.size.toString(), 0, latinDigits) }
    summary.arrayAt("labs")?.let { rows += "الفحوصات المخبرية" to number(it.size.toString(), 0, latinDigits) }
    summary.arrayAt("symptoms")?.let { rows += "الأعراض المسجلة" to number(it.size.toString(), 0, latinDigits) }

    return ReportSummaryPresentation(rows)
}

private fun JsonObject.objectAt(key: String): JsonObject = (this[key] as? JsonObject) ?: JsonObject(emptyMap())
private fun JsonObject.arrayAt(key: String): JsonArray? = this[key] as? JsonArray
private fun JsonObject.primitiveAt(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.displayValue(key: String): String? = primitiveAt(key)?.takeIf(String::isNotBlank)
private fun JsonObject.numberAt(key: String): Double? = primitiveAt(key)?.toDoubleOrNull()

private fun glucoseValue(mgDl: Double, unit: String?, latinDigits: Boolean): String {
    return if (unit == "MMOL_L") "${number((mgDl / 18.0182).toString(), 1, latinDigits)} mmol/L"
    else "${number(mgDl.toString(), 0, latinDigits)} mg/dL"
}

private fun number(value: String, decimals: Int, latinDigits: Boolean): String {
    val parsed = value.toDoubleOrNull() ?: return digits(value, latinDigits)
    val formatter = NumberFormat.getNumberInstance(if (latinDigits) Locale.US else Locale.forLanguageTag("ar-IQ"))
    formatter.minimumFractionDigits = if (decimals == 1) 1 else 0
    formatter.maximumFractionDigits = decimals
    return formatter.format(parsed)
}

private fun digits(value: String, latinDigits: Boolean): String = if (latinDigits) value else buildString {
    value.forEach { char -> append(if (char in '0'..'9') ('٠'.code + (char.code - '0'.code)).toChar() else char) }
}
