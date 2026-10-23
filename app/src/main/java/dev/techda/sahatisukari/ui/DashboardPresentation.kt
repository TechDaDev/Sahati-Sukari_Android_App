package dev.techda.sahatisukari.ui

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class DashboardGlucosePoint(val at: String, val epochMillis: Long, val value: Double)
internal data class DashboardGlucoseCard(val value: String, val context: String?, val dateTime: String?)
internal data class DashboardAdministrationCard(
    val status: String,
    val medicationName: String?,
    val dateTime: String?,
    val actualDose: String?,
)
internal data class DashboardActivityItem(val type: String, val duration: String, val intensity: String?, val time: String?)
internal data class DashboardEventDelta(
    val label: String,
    val at: String,
    val before: Double?,
    val after: Double?,
    val delta: Double?,
)
internal data class DashboardChartScale(val minimum: Double, val maximum: Double, val ticks: List<Double>)
internal data class DashboardChartTimeTick(val position: Float, val label: String)
internal data class DashboardSummaryLine(val label: String, val value: String)

internal object DashboardPresentation {
    fun latestGlucose(
        record: JsonObject?,
        preferredUnit: String,
        timeZoneId: String?,
        numeralSystem: String?,
    ): DashboardGlucoseCard? {
        record ?: return null
        val value = MedicalPresentation.formatGlucoseFromMgDl(record.text("standardizedValueMgdl"), preferredUnit, numeralSystem)
        val contextCode = record.text("measurementContext")
        val context = contextCode?.let { MedicalPresentation.label(MedicalEnumField.MEASUREMENT_CONTEXT, it) }
        val rawAt = record.text("measurementDatetime")
        val dateTime = rawAt?.let { MedicalPresentation.formatInstant(it, timeZoneId, numeralSystem) }
            ?.takeUnless { it == MedicalPresentation.UNAVAILABLE_DATE }
        return DashboardGlucoseCard(value, context, dateTime)
    }

    fun latestAdministration(
        record: JsonObject?,
        timeZoneId: String?,
        numeralSystem: String?,
    ): DashboardAdministrationCard? {
        record ?: return null
        val administration = record.objectAt("administration") ?: record
        val medication = record.objectAt("medication")
        val statusCode = administration.text("status")
        val status = MedicalPresentation.label(MedicalEnumField.ADMINISTRATION_STATUS, statusCode)
        val rawAt = administration.text("administrationDatetime")
        val dateTime = rawAt?.let { MedicalPresentation.formatInstant(it, timeZoneId, numeralSystem) }
            ?.takeUnless { it == MedicalPresentation.UNAVAILABLE_DATE }
        val dose = administration.text("actualDose")?.let { MedicalPresentation.formatDecimal(it, 2, numeralSystem) }
            ?.takeUnless { it == "—" }
        val unitCode = administration.text("doseUnit")
        val unit = unitCode?.let { MedicalPresentation.label(MedicalEnumField.DOSE_UNIT, it) }
        val actualDose = dose?.let { if (unit.isNullOrBlank() || unit == "غير متوفر") it else "$it $unit" }
        return DashboardAdministrationCard(status, medication?.text("medicationName"), dateTime, actualDose)
    }

    fun trendPoints(elements: JsonArray?): List<DashboardGlucosePoint> = elements.orEmpty().mapNotNull { element ->
        val record = element.objectOrNull() ?: return@mapNotNull null
        val at = record.text("at") ?: return@mapNotNull null
        val value = record.text("value")?.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return@mapNotNull null
        val millis = parseInstantMillis(at) ?: return@mapNotNull null
        DashboardGlucosePoint(at, millis, value)
    }.sortedWith(compareBy<DashboardGlucosePoint> { it.epochMillis }.thenBy { it.at })

    fun chartScale(points: List<DashboardGlucosePoint>, preferredUnit: String): DashboardChartScale? {
        if (points.isEmpty()) return null
        val dataMin = points.minOf { it.value }
        val dataMax = points.maxOf { it.value }
        val initialRange = dataMax - dataMin
        val padding = if (initialRange == 0.0) maxOf(abs(dataMin) * 0.1, if (preferredUnit == "MMOL_L") 0.5 else 10.0) else initialRange * 0.1
        val rawRange = (initialRange + 2 * padding).coerceAtLeast(if (preferredUnit == "MMOL_L") 0.5 else 1.0)
        val step = niceStep(rawRange / 4.0)
        val minimum = floor((dataMin - padding) / step) * step
        val maximum = ceil((dataMax + padding) / step) * step
        val ticks = (0..5).map { minimum + (maximum - minimum) * it / 5.0 }
        return DashboardChartScale(minimum, maximum, ticks)
    }

    fun chartTimePosition(point: DashboardGlucosePoint, points: List<DashboardGlucosePoint>): Float {
        if (points.size < 2) return 0.5f
        val first = points.first().epochMillis
        val last = points.last().epochMillis
        if (last <= first) return 0.5f
        return ((point.epochMillis - first).toDouble() / (last - first)).toFloat().coerceIn(0f, 1f)
    }

    fun glucoseUnitLabel(unit: String): String = if (unit == "MMOL_L") "mmol/L" else "mg/dL"

    /** Mirrors the website's low/high entry-confirmation note; these are not clinical thresholds. */
    fun glucoseInformationNote(valueMgDl: String?, numeralSystem: String?): String? {
        val value = valueMgDl?.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
        val direction = when {
            value < 60.0 -> "أقل"
            value > 400.0 -> "أعلى"
            else -> return null
        }
        val shown = MedicalPresentation.formatDecimal(value.toString(), 0, numeralSystem)
        return "القراءة المسجَّلة ($shown mg/dL) $direction من المستويات المعتادة. يفضّل التحقق من دقة القياس وإبلاغ فريقك الطبي بها في الزيارة القادمة. إذا كنت تشعر بأعراض شديدة أو متزايدة، أو إذا كان لديك شك في حالة طارئة، توجّه فورًا إلى أقرب خدمة طبية أو اتصل بالطوارئ."
    }

    fun todaySummary(dashboard: JsonObject, numeralSystem: String?): List<DashboardSummaryLine> {
        val latestSleep = dashboard.arrayAt("lastSleep")?.firstOrNull()?.objectOrNull()
        val latestWeight = dashboard.arrayAt("latestWeight")?.firstOrNull()?.objectOrNull()
        val latestHba1c = dashboard.arrayAt("latestHba1c")?.firstOrNull()?.objectOrNull()
        val mealCount = dashboard.text("mealsToday")?.let { MedicalPresentation.formatDecimal(it, 0, numeralSystem) } ?: "—"
        val symptomCount = dashboard.arrayAt("todaySymptoms")?.size?.let { MedicalPresentation.localizedDigits(it.toString(), numeralSystem) } ?: "—"
        val sleep = latestSleep?.text("durationMinutes")?.toIntOrNull()?.takeIf { it >= 0 }
            ?.let { MedicalPresentation.formatSleepDuration(it.toString(), numeralSystem) } ?: "—"
        val weight = latestWeight?.text("weightKg")?.let { MedicalPresentation.formatWeight(it, numeralSystem) } ?: "—"
        val hba1c = latestHba1c?.text("value")?.let { MedicalPresentation.formatHbA1c(it, numeralSystem) } ?: "—"
        return listOf(
            DashboardSummaryLine("الوجبات المسجّلة", mealCount),
            DashboardSummaryLine("الأعراض المسجّلة", symptomCount),
            DashboardSummaryLine("آخر نوم مسجّل", sleep),
            DashboardSummaryLine("الوزن الأخير", weight),
            DashboardSummaryLine("آخر HbA1c مسجّل", hba1c),
        )
    }

    fun chartAxisLabels(points: List<DashboardGlucosePoint>, timeZoneId: String?, numeralSystem: String?): List<DashboardChartTimeTick> {
        if (points.isEmpty()) return emptyList()
        val middleIndex = points.size / 2
        val includeMiddle = points.size > 2 && chartTimePosition(points[middleIndex], points) in 0.2f..0.8f
        val indexes = if (points.size <= 2) points.indices.toList() else if (includeMiddle) listOf(0, middleIndex, points.lastIndex).distinct() else listOf(0, points.lastIndex)
        val zone = runCatching { ZoneId.of(timeZoneId ?: "Asia/Baghdad") }.getOrElse { ZoneId.of("Asia/Baghdad") }
        val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.ROOT)
        return indexes.mapNotNull { points.getOrNull(it) }.map { point ->
            val text = runCatching { formatter.format(Instant.ofEpochMilli(point.epochMillis).atZone(zone)) }.getOrDefault("—")
            DashboardChartTimeTick(chartTimePosition(point, points), MedicalPresentation.localizedDigits(text, numeralSystem))
        }
    }

    /** Same open/closed interval bounds and three-hour windows as the website. */
    fun windowAverageDelta(points: List<DashboardGlucosePoint>, eventAt: String): Triple<Double?, Double?, Double?> {
        val instant = parseInstantMillis(eventAt) ?: return Triple(null, null, null)
        val before = points.filter { it.epochMillis >= instant - THREE_HOURS_MS && it.epochMillis < instant }.map { it.value }
        val after = points.filter { it.epochMillis > instant && it.epochMillis <= instant + THREE_HOURS_MS }.map { it.value }
        val beforeAverage = before.averageOrNull()
        val afterAverage = after.averageOrNull()
        return Triple(beforeAverage, afterAverage, if (beforeAverage != null && afterAverage != null) afterAverage - beforeAverage else null)
    }

    fun eventDeltas(dashboard: JsonObject, timeZoneId: String?, numeralSystem: String?): List<DashboardEventDelta> {
        val points = trendPoints(dashboard.arrayAt("trendData"))
        val events = buildList {
            dashboard.arrayAt("recentAdministrations").orEmpty().take(3).forEach { item ->
                val record = item.objectOrNull() ?: return@forEach
                val administration = record.objectAt("administration") ?: return@forEach
                val at = administration.text("administrationDatetime") ?: return@forEach
                val name = record.objectAt("medication")?.text("medicationName") ?: "دواء مسجّل"
                add("دواء: $name" to at)
            }
            dashboard.arrayAt("recentMeals").orEmpty().take(2).forEach { item ->
                val record = item.objectOrNull() ?: return@forEach
                val at = record.text("mealDatetime") ?: return@forEach
                val meal = record.text("mealType")?.let { MedicalPresentation.label(MedicalEnumField.MEAL_TYPE, it) } ?: "وجبة مسجّلة"
                add("وجبة: $meal" to at)
            }
        }
        return events.map { (label, at) ->
            val (before, after, delta) = windowAverageDelta(points, at)
            DashboardEventDelta(label, MedicalPresentation.formatInstant(at, timeZoneId, numeralSystem), before, after, delta)
        }
    }

    fun todayActivities(elements: JsonArray?, timeZoneId: String?, numeralSystem: String?): List<DashboardActivityItem> =
        elements.orEmpty().mapNotNull { item ->
            val record = item.objectOrNull() ?: return@mapNotNull null
            val type = record.text("activityType")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val duration = record.text("durationMinutes")?.toIntOrNull()?.takeIf { it >= 0 }
                ?.let { MedicalPresentation.formatActivityDuration(it.toString(), numeralSystem) } ?: return@mapNotNull null
            val intensity = record.text("intensity")?.let { MedicalPresentation.label(MedicalEnumField.ACTIVITY_INTENSITY, it) }
            val at = record.text("activityDatetime")
            val time = at?.let { MedicalPresentation.formatInstant(it, timeZoneId, numeralSystem) }
                ?.takeUnless { it == MedicalPresentation.UNAVAILABLE_DATE }
            DashboardActivityItem(type, duration, intensity, time)
        }

    fun recentGlucose(elements: JsonArray?, preferredUnit: String, timeZoneId: String?, numeralSystem: String?): List<DashboardGlucoseCard> =
        elements.orEmpty().mapNotNull { item ->
            val record = item.objectOrNull() ?: return@mapNotNull null
            latestGlucose(record, preferredUnit, timeZoneId, numeralSystem)
                ?.let { card -> card.copy(context = card.context ?: "سياق غير مسجّل") }
        }.takeLast(6).asReversed()

    private fun JsonElement.objectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()
    private fun JsonObject.text(key: String): String? = runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()
    private fun JsonObject.objectAt(key: String): JsonObject? = this[key]?.objectOrNull()
    private fun JsonObject.arrayAt(key: String): JsonArray? = runCatching { this[key] as? JsonArray }.getOrNull()
    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average().takeIf(Double::isFinite)

    private fun niceStep(value: Double): Double {
        if (!value.isFinite() || value <= 0.0) return 1.0
        val magnitude = 10.0.pow(floor(log10(value)))
        val normalized = value / magnitude
        val nice = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    private fun parseInstantMillis(value: String): Long? = runCatching { Instant.parse(value).toEpochMilli() }.getOrElse {
        runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
    }

    private const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L
}

@Composable
internal fun DashboardTrendChart(
    points: List<DashboardGlucosePoint>,
    glucoseUnit: String,
    timeZoneId: String?,
    numeralSystem: String?,
) {
    if (points.isEmpty()) {
        Text(
            "لا توجد قراءات مسجّلة خلال آخر 14 يومًا.",
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
        return
    }
    val scale = DashboardPresentation.chartScale(points, glucoseUnit) ?: return
    val unitLabel = DashboardPresentation.glucoseUnitLabel(glucoseUnit)
    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val labels = DashboardPresentation.chartAxisLabels(points, timeZoneId, numeralSystem)
    val description = buildString {
        append("مخطط قراءات السكر خلال آخر 14 يومًا، بوحدة ").append(unitLabel)
        append("، عدد القراءات ").append(MedicalPresentation.localizedDigits(points.size.toString(), numeralSystem))
        append("، من ").append(labels.first().label).append(" إلى ").append(labels.last().label)
        append("، أقل قراءة ").append(MedicalPresentation.formatDecimal(minValue.toString(), if (glucoseUnit == "MMOL_L") 1 else 2, numeralSystem))
        append("، أعلى قراءة ").append(MedicalPresentation.formatDecimal(maxValue.toString(), if (glucoseUnit == "MMOL_L") 1 else 2, numeralSystem))
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val density = LocalDensity.current
    Column(Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Text("قيمة السكر ($unitLabel)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        if (points.size == 1) {
            Text("قراءة واحدة مسجّلة؛ لا تكفي لرسم اتجاه زمني.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(208.dp)) {
            val left = 48.dp.toPx()
            val right = 8.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = 40.dp.toPx()
            val plotWidth = (size.width - left - right).coerceAtLeast(1f)
            val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
            val nativePaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                color = labelColor
                textAlign = NativePaint.Align.CENTER
                textSize = with(density) { 9.sp.toPx() }
            }
            scale.ticks.forEach { tick ->
                val ratio = ((tick - scale.minimum) / (scale.maximum - scale.minimum)).toFloat()
                val y = top + plotHeight * (1f - ratio)
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(size.width - right, y), strokeWidth = 1.dp.toPx())
                val decimals = if (glucoseUnit == "MMOL_L") 1 else 0
                val label = MedicalPresentation.formatDecimal(tick.toString(), decimals, numeralSystem)
                drawContext.canvas.nativeCanvas.drawText(label, left / 2f, y + nativePaint.textSize * 0.35f, nativePaint)
            }
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + plotHeight), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, top + plotHeight), androidx.compose.ui.geometry.Offset(size.width - right, top + plotHeight), strokeWidth = 1.dp.toPx())

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = left + plotWidth * DashboardPresentation.chartTimePosition(point, points)
                val y = top + plotHeight * (1f - ((point.value - scale.minimum) / (scale.maximum - scale.minimum)).toFloat())
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(lineColor, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
            if (points.size > 1) drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

            labels.forEach { tick ->
                val x = left + plotWidth * tick.position
                val split = tick.label.split(' ')
                drawContext.canvas.nativeCanvas.drawText(split.firstOrNull().orEmpty(), x, size.height - 19.dp.toPx(), nativePaint)
                drawContext.canvas.nativeCanvas.drawText(split.getOrNull(1).orEmpty(), x, size.height - 5.dp.toPx(), nativePaint)
            }
        }
        Text("التاريخ والوقت", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
