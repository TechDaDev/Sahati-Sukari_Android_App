package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class HealthProfileLine(val label: String, val value: String)
internal data class HealthHistoryRecord(val title: String, val fields: List<HealthProfileLine>)
internal data class HealthProfileHistorySection(val endpoint: String, val title: String)

internal fun healthProfileHistorySections(): List<HealthProfileHistorySection> =
    listOf(HealthProfileHistorySection("labs", "التحاليل المخبرية"))

internal fun healthHistoryPresentation(endpoint: String, response: JsonObject?): List<HealthHistoryRecord> {
    if (endpoint !in setOf("weight", "labs")) return emptyList()
    val rows = response?.get("items") as? JsonArray ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        fun value(key: String) = row[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        val fields = buildList {
            if (endpoint == "weight") {
                value("weightKg")?.let { add(HealthProfileLine("الوزن", MedicalPresentation.formatWeight(it))) }
                value("measurementDate")?.let { add(HealthProfileLine("تاريخ القياس", formatProfileDate(it, null))) }
                value("notes")?.let { add(HealthProfileLine("ملاحظات", it)) }
            } else {
                val testType = value("testType")
                testType?.let { add(HealthProfileLine("نوع التحليل", MedicalPresentation.label(MedicalEnumField.LAB_TEST_TYPE, it))) }
                val primaryValue = value("value")
                val secondaryValue = value("valueSecondary")
                if (testType == "BLOOD_PRESSURE" && primaryValue != null && secondaryValue != null) {
                    add(HealthProfileLine("النتيجة", MedicalPresentation.formatBloodPressure(primaryValue, secondaryValue)))
                } else {
                    primaryValue?.let { add(HealthProfileLine("النتيجة", MedicalPresentation.formatLabValue(it))) }
                }
                if (testType != "BLOOD_PRESSURE") secondaryValue?.let { add(HealthProfileLine("قيمة إضافية", MedicalPresentation.formatLabValue(it))) }
                value("unit")?.let { add(HealthProfileLine("الوحدة", it)) }
                value("testDate")?.let { add(HealthProfileLine("تاريخ التحليل", formatProfileDate(it, null))) }
                value("referenceRange")?.let { add(HealthProfileLine("المدى المرجعي", it)) }
                value("notes")?.let { add(HealthProfileLine("ملاحظات", it)) }
            }
        }
        if (fields.isEmpty()) return@mapNotNull null
        HealthHistoryRecord(fields.first().value, fields)
    }
}

internal data class HealthConditionLine(
    val typeCode: String,
    val statusCode: String,
    val label: String,
    val status: String,
    val notes: String?,
)

internal data class HealthProfilePresentation(
    val summary: List<HealthProfileLine>,
    val identity: List<HealthProfileLine>,
    val completionPercent: Int?,
    val diabetes: List<HealthProfileLine>,
    val conditions: List<HealthConditionLine>,
    val reproductive: List<HealthProfileLine>,
    val additional: List<HealthProfileLine>,
    val preferences: List<HealthProfileLine>,
) {
    val hasVisibleContent: Boolean
        get() = summary.isNotEmpty() || identity.isNotEmpty() || completionPercent != null || diabetes.isNotEmpty() ||
            conditions.isNotEmpty() || reproductive.isNotEmpty() || additional.isNotEmpty() || preferences.isNotEmpty()
}

/** Converts the API's flat profile response into an allow-listed Arabic presentation model. */
internal fun healthProfilePresentation(root: JsonObject?): HealthProfilePresentation {
    if (root == null) return HealthProfilePresentation(emptyList(), emptyList(), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    val profile = root.objectAt("profile") ?: root
    val numeralSystem = profile.stringAt("numeralSystem") ?: root.stringAt("numeralSystem")

    fun line(label: String, key: String, transform: (String) -> String = { it }): HealthProfileLine? =
        profile.stringAt(key)?.takeIf(String::isNotBlank)?.let { HealthProfileLine(label, transform(it)) }

    fun sectionLines(source: JsonObject?, fields: List<Pair<String, String>>): List<HealthProfileLine> =
        fields.mapNotNull { (key, label) ->
            source?.stringAt(key)?.takeIf(String::isNotBlank)?.let { raw ->
                HealthProfileLine(label, profileEnumLabel(key, raw))
            }
        }

    val summary = buildList {
        line("الاسم", "fullName")?.let(::add)
        line("الجنس", "gender", ::profileEnumLabelForGender)?.let(::add)
        profile.stringAt("age")?.let { add(HealthProfileLine("العمر", "${localizedDigits(it, numeralSystem)} سنة")) }
        line("تاريخ الميلاد", "dateOfBirth") { formatProfileDate(it, numeralSystem) }?.let(::add)
        profile.stringAt("heightCm")?.let { add(HealthProfileLine("الطول", "${localizedDigits(it, numeralSystem)} سم")) }
        profile.stringAt("currentWeightKg")?.let { add(HealthProfileLine("الوزن الحالي", MedicalPresentation.formatWeight(it, numeralSystem))) }
        profile.stringAt("bmi")?.let { add(HealthProfileLine("مؤشر كتلة الجسم", MedicalPresentation.formatBmi(it, numeralSystem))) }
    }

    val identity = buildList {
        profile.stringAt("email")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("البريد الإلكتروني", it)) }
        profile.stringAt("nationalIdMasked")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("الرقم الوطني المسجّل", it)) }
        profile.stringAt("phoneNumber")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("رقم الهاتف", it)) }
        profile.stringAt("emailVerified")?.let { add(HealthProfileLine("حالة البريد الإلكتروني", if (it == "true") "تم التحقق" else "لم يتم التحقق")) }
        profile.stringAt("phoneVerified")?.let { add(HealthProfileLine("حالة الهاتف", if (it == "true") "تم التحقق" else "لم يتم التحقق")) }
    }

    val completion = profile.objectAt("completion") ?: root.objectAt("completion")
    val completionPercent = completion?.stringAt("percent")?.toIntOrNull()?.coerceIn(0, 100)

    val diabetesObject = root.objectAt("diabetes")
    val diabetes = buildList {
        diabetesObject?.stringAt("diabetesType")?.let { add(HealthProfileLine("نوع السكري", profileEnumLabel("diabetesType", it))) }
        diabetesObject?.stringAt("diagnosisDate")?.let { add(HealthProfileLine("تاريخ التشخيص", formatProfileDate(it, numeralSystem))) }
        diabetesObject?.stringAt("diagnosisYear")?.let { add(HealthProfileLine("سنة التشخيص", localizedDigits(it, numeralSystem))) }
        listOf(
            "monitoringMethod" to "طريقة مراقبة السكر",
            "hypoglycemiaHistory" to "تاريخ هبوط السكر",
            "hypoglycemiaAwareness" to "الإحساس بهبوط السكر",
            "treatmentApproach" to "أسلوب العلاج الحالي",
        ).forEach { (key, label) ->
            diabetesObject?.stringAt(key)?.let { add(HealthProfileLine(label, profileEnumLabel(key, it))) }
        }
        diabetesObject?.stringAt("notes")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("ملاحظات حول السكري", it)) }
    }

    val conditionItems = root.arrayAt("conditions")?.mapNotNull { element ->
        val item = element.objectOrNull() ?: return@mapNotNull null
        val typeCode = item.stringAt("conditionType") ?: return@mapNotNull null
        val statusCode = item.stringAt("status") ?: return@mapNotNull null
        HealthConditionLine(
            typeCode = typeCode,
            statusCode = statusCode,
            label = conditionTypeLabel(typeCode),
            status = conditionStatusLabel(statusCode),
            notes = item.stringAt("notes")?.takeIf(String::isNotBlank),
        )
    }.orEmpty()

    val reproductiveObject = root.objectAt("reproductive")
    val reproductive = sectionLines(
        reproductiveObject,
        listOf("pregnancyStatus" to "الحالة الحالية", "gestationalDiabetesHistory" to "تاريخ سكري الحمل"),
    ) + listOfNotNull(reproductiveObject?.stringAt("notes")?.takeIf(String::isNotBlank)?.let { HealthProfileLine("ملاحظات", it) })

    val additional = buildList {
        profile.stringAt("allergies")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("حساسية من أدوية معينة", it)) }
        profile.stringAt("smokingStatus")?.let { add(HealthProfileLine("حالة التدخين", profileEnumLabel("smokingStatus", it))) }
        profile.stringAt("familyHistoryDiabetes")?.let { add(HealthProfileLine("تاريخ عائلي للسكري", profileEnumLabel("familyHistoryDiabetes", it))) }
        profile.stringAt("healthNotes")?.takeIf(String::isNotBlank)?.let { add(HealthProfileLine("معلومات صحية إضافية", it)) }
    }

    val preferences = buildList {
        profile.stringAt("preferredGlucoseUnit")?.let { add(HealthProfileLine("وحدة السكر المفضلة", profileEnumLabel("preferredGlucoseUnit", it))) }
        profile.stringAt("numeralSystem")?.let { add(HealthProfileLine("نظام الأرقام", profileEnumLabel("numeralSystem", it))) }
    }

    return HealthProfilePresentation(summary, identity, completionPercent, diabetes, conditionItems, reproductive, additional, preferences)
}

internal fun conditionTypeLabel(code: String): String = profileEnumLabel("conditionType", code)

internal fun conditionStatusLabel(code: String): String = profileEnumLabel("conditionStatus", code)

internal fun profileEnumLabel(field: String, code: String): String = profileMedicalEnumLabel(field, code)

@Composable
internal fun HealthProfileContent(
    model: HealthProfilePresentation,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SahatiUiTokens.sectionSpacing)) {
        if (model.completionPercent != null) {
            SahatiProfileCard("الملخص الأساسي") {
                ProfileLines(model.summary)
                Text("نسبة اكتمال الملف الصحي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { model.completionPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("${model.completionPercent}٪", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, style = MaterialTheme.typography.labelMedium)
            }
        } else if (model.summary.isNotEmpty()) {
            SahatiProfileCard("الملخص الأساسي") { ProfileLines(model.summary) }
        }
        if (model.identity.isNotEmpty()) SahatiProfileCard("بيانات الهوية والتواصل") { ProfileLines(model.identity) }
        if (model.diabetes.isNotEmpty()) SahatiProfileCard("معلومات السكري") { ProfileLines(model.diabetes) }
        if (model.conditions.isNotEmpty()) {
            SahatiProfileCard("الأمراض والحالات المصاحبة") {
                model.conditions.forEach { condition ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(condition.label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
                            Text(condition.status, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
                        }
                        condition.notes?.let { Text(it, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start) }
                    }
                }
                Text(
                    "حالة «غير مُشخّصة» تعكس السجل فقط ولا تنفي وجود الحالة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }
        if (model.reproductive.isNotEmpty()) SahatiProfileCard("معلومات الحمل أو سكري الحمل (عند ملاءمتها)") { ProfileLines(model.reproductive) }
        if (model.additional.isNotEmpty()) SahatiProfileCard("معلومات إضافية") { ProfileLines(model.additional) }
        if (model.preferences.isNotEmpty()) SahatiProfileCard("التفضيلات الصحية") { ProfileLines(model.preferences) }
        if (!model.hasVisibleContent) {
            SahatiProfileCard("ملفي الصحي") {
                Text("لم ترد بيانات الملف الصحي بعد.", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                androidx.compose.material3.OutlinedButton(onClick = onReload) { Text("إعادة المحاولة") }
            }
        }
    }
}

@Composable
private fun SahatiProfileCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SahatiUiTokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start)
            content()
        }
    }
}

@Composable
private fun ProfileLines(lines: List<HealthProfileLine>) {
    lines.forEach { line ->
        MedicalLabelValueRow(
            label = line.label,
            value = line.value,
            labelModifier = Modifier.padding(end = 10.dp),
            valueWeight = 1f,
            labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            valueStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun JsonObject.stringAt(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    ?.takeIf { it != "null" }

private fun JsonObject.objectAt(key: String): JsonObject? = (this[key] as? JsonObject)

private fun JsonObject.arrayAt(key: String): JsonArray? = this[key] as? JsonArray

private fun JsonElement.objectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

private fun profileEnumLabelForGender(value: String): String = profileEnumLabel("gender", value)

private fun formatProfileDate(value: String, numeralSystem: String?): String = MedicalPresentation.formatDateOnly(value, numeralSystem)

private fun localizedDigits(value: String, numeralSystem: String?): String = MedicalPresentation.localizedDigits(value, numeralSystem)
