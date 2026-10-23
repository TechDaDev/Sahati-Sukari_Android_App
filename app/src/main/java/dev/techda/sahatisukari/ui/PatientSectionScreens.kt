package dev.techda.sahatisukari.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class FieldSpec(
    val key: String,
    val label: String,
    val options: List<String> = emptyList(),
    val numeric: Boolean = false,
    val required: Boolean = true,
    val defaultValue: String = "",
)

private val glucoseContexts = listOf("FASTING", "BEFORE_BREAKFAST", "AFTER_BREAKFAST", "BEFORE_LUNCH", "AFTER_LUNCH", "BEFORE_DINNER", "AFTER_DINNER", "BEFORE_SLEEP", "OTHER")
private val doseUnits = listOf("UNIT", "MG", "MCG", "G", "ML", "TABLET", "CAPSULE", "DROP", "PUFF", "SACHET")

@Composable
fun PatientSectionScreen(section: Int, modifier: Modifier = Modifier, viewModel: PatientSectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val endpoint = when (section) { 2 -> "glucose"; 3 -> "medications"; else -> "profile" }
    LaunchedEffect(endpoint) {
        viewModel.load(endpoint)
        if (endpoint == "profile") viewModel.loadProfileHistory()
    }
    LaunchedEffect(section) { if (section == 3) viewModel.loadRelated("administrations") }
    if (section == 4) {
        JournalScreen(modifier, viewModel)
        return
    }
    if (section == 5) {
        ReportsScreen(modifier)
        return
    }
    SectionContent(section, state, modifier, viewModel)
}

@Composable
private fun SectionContent(section: Int, state: PatientSectionState, modifier: Modifier, viewModel: PatientSectionViewModel) {
    val endpoint = when (section) { 1 -> "profile"; 2 -> "glucose"; else -> "medications" }
    val title = when (section) { 1 -> "ملفي الصحي"; 2 -> "قياسات السكر"; else -> "أدويتي" }
    var adding by remember(endpoint) { mutableStateOf(false) }
    var addingDose by remember(section) { mutableStateOf(false) }
    var editing by remember(endpoint) { mutableStateOf<Pair<String, JsonObject>?>(null) }
    var profileEditing by remember(section) { mutableStateOf(false) }
    LaunchedEffect(state.notice) { if (section == 1 && state.notice == "تم الحفظ.") profileEditing = false }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SahatiPageTitle(title)
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
        state.notice?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start) }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else if (section == 1) {
            if (profileEditing) ProfileEditor(state.data, state.busy, viewModel::save)
            else {
                Button(onClick = { profileEditing = true }, modifier = Modifier.fillMaxWidth()) { Text("تعديل الملف الصحي") }
                ProfileContent(
                    state.data,
                    state.profileWeights,
                    state.profileLabs,
                    state.profileHistoryLoading,
                    state.busy,
                    viewModel::reload,
                    viewModel::save,
                    viewModel::loadMoreProfileHistory,
                )
            }
        }
        else {
            if (section == 2) Text("سجّل قراءاتك كما ظهرت على الجهاز. لا يقدم التطبيق تفسيرًا طبيًا للقراءات.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            Button(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) { Text(if (adding) "إلغاء" else if (section == 2) "إضافة قياس" else "إضافة دواء") }
            if (adding) RecordForm(endpoint, state.busy, timeZoneId = state.patientTimezone) { body -> viewModel.save(endpoint, body); adding = false }
            editing?.let { (id, record) -> RecordForm(endpoint, state.busy, record, state.patientTimezone) { body -> viewModel.save(endpoint, body, id); editing = null } }
            RecordList(endpoint, state.data, onDelete = viewModel::delete, onEdit = { id, record -> editing = id to record }, timeZoneId = state.patientTimezone, numeralSystem = state.numeralSystem)
            PaginationAction(state.data, state.busy, viewModel::loadMore)
            if (section == 3) {
                Text("تسجيل الدواء لا يغيّر الجرعة الموصوفة. سجّل الجرعة الفعلية التي تناولتها فقط.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
                Text("سجل الجرعات", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
                Button(onClick = { addingDose = !addingDose }, modifier = Modifier.fillMaxWidth()) { Text(if (addingDose) "إلغاء" else "تسجيل جرعة") }
                if (addingDose) AdministrationForm(state.data, state.busy) { body -> viewModel.save("administrations", body); addingDose = false }
                RecordList("administrations", state.relatedData, onDelete = viewModel::delete, timeZoneId = state.patientTimezone, numeralSystem = state.numeralSystem)
                PaginationAction(state.relatedData, state.busy, viewModel::loadRelatedMore)
            }
        }
        OutlinedButton(onClick = viewModel::reload, modifier = Modifier.fillMaxWidth()) { Text("تحديث") }
    }
}

@Composable
private fun JournalScreen(modifier: Modifier, viewModel: PatientSectionViewModel) {
    var endpoint by remember { mutableStateOf("meals") }
    val state by viewModel.state.collectAsState()
    var adding by remember(endpoint) { mutableStateOf(false) }
    LaunchedEffect(endpoint) { viewModel.load(endpoint) }
    val choices = listOf("meals" to "الوجبات", "activities" to "النشاط", "sleep" to "النوم", "symptoms" to "الأعراض", "factors" to "العوامل اليومية", "weight" to "الوزن", "labs" to "التحاليل")
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SahatiPageTitle("يومياتي")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { (key, title) -> FilterChip(selected = endpoint == key, onClick = { endpoint = key }, label = { Text(title) }) }
        }
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else {
            Button(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) { Text(if (adding) "إلغاء" else "إضافة سجل") }
            if (adding) RecordForm(endpoint, state.busy) { body -> viewModel.save(endpoint, body); adding = false }
            RecordList(endpoint, state.data, onDelete = viewModel::delete)
            PaginationAction(state.data, state.busy, viewModel::loadMore)
            OutlinedButton(onClick = viewModel::reload, modifier = Modifier.fillMaxWidth()) { Text("تحديث") }
        }
    }
}

@Composable
private fun PaginationAction(data: JsonObject?, busy: Boolean, onLoadMore: () -> Unit) {
    val more = runCatching { data?.get("pagination")?.jsonObject?.get("hasMore")?.jsonPrimitive?.content == "true" }.getOrDefault(false)
    if (more) OutlinedButton(onClick = onLoadMore, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "جارٍ التحميل…" else "تحميل المزيد") }
}

@Composable
private fun ProfileContent(
    data: JsonObject?, weights: JsonObject?, labs: JsonObject?, historyLoading: Boolean, busy: Boolean,
    onReload: () -> Unit, onSave: (String, JsonObject) -> Unit,
    onLoadMore: (String) -> Unit,
) {
    var addingWeight by remember { mutableStateOf(false) }
    var addingLab by remember { mutableStateOf(false) }
    HealthProfileContent(healthProfilePresentation(data), onReload)
    ProfileHistorySection("سجل الوزن", "weight", weights, historyLoading, busy, addingWeight, { addingWeight = it }, onSave, onLoadMore)
    ProfileHistorySection("التحاليل المخبرية", "labs", labs, historyLoading, busy, addingLab, { addingLab = it }, onSave, onLoadMore)
}

@Composable
private fun ProfileHistorySection(
    title: String, endpoint: String, data: JsonObject?, loading: Boolean, busy: Boolean,
    adding: Boolean, setAdding: (Boolean) -> Unit, onSave: (String, JsonObject) -> Unit,
    onLoadMore: (String) -> Unit,
) {
    val items = (data?.get("items") as? JsonArray).orEmpty()
    val records = healthHistoryPresentation(endpoint, data)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.End) {
            Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
            OutlinedButton(onClick = { setAdding(!adding) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (adding) "إلغاء" else if (endpoint == "weight") "إضافة قياس وزن" else "إضافة تحليل")
            }
            if (adding) RecordForm(endpoint, busy) { body -> onSave(endpoint, body); setAdding(false) }
            if (loading && data == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            else if (items.isEmpty()) Text(if (loading) "جارٍ تحميل السجل…" else "لا توجد سجلات لعرضها.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            records.forEach { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        Text(record.title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Start)
                        record.fields.forEach { field ->
                            Text("${field.label}: ${field.value}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
                        }
                    }
                }
            }
            val pagination = data?.get("pagination")?.let { runCatching { it.jsonObject }.getOrNull() }
            if (pagination?.get("nextCursor")?.jsonPrimitive?.contentOrNull != null) {
                OutlinedButton(onClick = { onLoadMore(endpoint) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("عرض المزيد") }
            }
        }
    }
}

@Composable
internal fun ProfileEditor(data: JsonObject?, busy: Boolean, onSave: (String, JsonObject) -> Unit) {
    val areas = listOf("profile" to "معلومات أساسية", "diabetes" to "السكري", "conditions" to "الحالات الصحية", "reproductive" to "المعلومات الإنجابية", "additional" to "معلومات إضافية", "preferences" to "التفضيلات الصحية")
    var area by remember { mutableStateOf("profile") }
    val root = data ?: JsonObject(emptyMap())
    val sectionData = when (area) {
        "diabetes" -> root["diabetes"]?.let { runCatching { it.jsonObject }.getOrNull() }
        "reproductive" -> root["reproductive"]?.let { runCatching { it.jsonObject }.getOrNull() }
        "additional", "preferences" -> root["profile"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: root
        else -> root["profile"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: root
    }
    val fields = when (area) {
        "profile" -> listOf(FieldSpec("heightCm", "الطول بالسنتيمتر", numeric = true, required = false, defaultValue = sectionData?.get("heightCm")?.jsonPrimitive?.contentOrNull.orEmpty()))
        "diabetes" -> listOf(
            FieldSpec("diabetesType", "نوع السكري", listOf("TYPE_1", "TYPE_2", "OTHER", "UNKNOWN"), required = false, defaultValue = sectionData?.get("diabetesType")?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"),
            FieldSpec("diagnosisDate", "تاريخ التشخيص (اختياري)", required = false, defaultValue = sectionData?.get("diagnosisDate")?.jsonPrimitive?.contentOrNull.orEmpty()),
            FieldSpec("diagnosisYear", "سنة التشخيص (اختياري)", numeric = true, required = false, defaultValue = sectionData?.get("diagnosisYear")?.jsonPrimitive?.contentOrNull.orEmpty()),
            FieldSpec("monitoringMethod", "طريقة المتابعة", listOf("FINGERSTICK", "CGM", "BOTH", "NOT_SPECIFIED"), required = false, defaultValue = sectionData?.get("monitoringMethod")?.jsonPrimitive?.contentOrNull ?: "NOT_SPECIFIED"),
            FieldSpec("hypoglycemiaHistory", "تاريخ هبوط السكر", listOf("YES", "NO", "UNSURE"), required = false, defaultValue = sectionData?.get("hypoglycemiaHistory")?.jsonPrimitive?.contentOrNull ?: "UNSURE"),
            FieldSpec("hypoglycemiaAwareness", "الإحساس بهبوط السكر", listOf("NORMAL", "REDUCED", "UNSURE", "NOT_SPECIFIED"), required = false, defaultValue = sectionData?.get("hypoglycemiaAwareness")?.jsonPrimitive?.contentOrNull ?: "NOT_SPECIFIED"),
            FieldSpec("treatmentApproach", "أسلوب العلاج", listOf("INSULIN", "NON_INSULIN_MEDICATION", "BOTH", "LIFESTYLE_ONLY", "NOT_SPECIFIED"), required = false, defaultValue = sectionData?.get("treatmentApproach")?.jsonPrimitive?.contentOrNull ?: "NOT_SPECIFIED"),
            FieldSpec("notes", "ملاحظات", required = false, defaultValue = sectionData?.get("notes")?.jsonPrimitive?.contentOrNull.orEmpty()),
        )
        "conditions" -> conditionFields(root)
        "reproductive" -> listOf(
            FieldSpec("pregnancyStatus", "حالة الحمل", listOf("NOT_APPLICABLE", "NOT_PREGNANT", "PREGNANT", "BREASTFEEDING", "NOT_SPECIFIED"), defaultValue = sectionData?.get("pregnancyStatus")?.jsonPrimitive?.contentOrNull ?: "NOT_SPECIFIED"),
            FieldSpec("gestationalDiabetesHistory", "تاريخ سكري الحمل", listOf("YES", "NO", "UNSURE"), defaultValue = sectionData?.get("gestationalDiabetesHistory")?.jsonPrimitive?.contentOrNull ?: "UNSURE"),
            FieldSpec("notes", "ملاحظات", required = false, defaultValue = sectionData?.get("notes")?.jsonPrimitive?.contentOrNull.orEmpty()),
        )
        "preferences" -> listOf(
            FieldSpec("preferredGlucoseUnit", "وحدة السكر المفضلة", listOf("MG_DL", "MMOL_L"), required = false, defaultValue = sectionData?.get("preferredGlucoseUnit")?.jsonPrimitive?.contentOrNull ?: "MG_DL"),
            FieldSpec("numeralSystem", "نظام الأرقام", listOf("LATIN", "ARABIC_INDIC"), required = false, defaultValue = sectionData?.get("numeralSystem")?.jsonPrimitive?.contentOrNull ?: "LATIN"),
        )
        else -> listOf(
            FieldSpec("allergies", "الحساسية", required = false, defaultValue = sectionData?.get("allergies")?.jsonPrimitive?.contentOrNull.orEmpty()),
            FieldSpec("smokingStatus", "حالة التدخين", listOf("NEVER", "CURRENT", "FORMER", "NOT_SPECIFIED"), required = false, defaultValue = sectionData?.get("smokingStatus")?.jsonPrimitive?.contentOrNull ?: "NOT_SPECIFIED"),
            FieldSpec("familyHistoryDiabetes", "تاريخ عائلي للسكري", listOf("YES", "NO", "UNSURE"), required = false, defaultValue = sectionData?.get("familyHistoryDiabetes")?.jsonPrimitive?.contentOrNull ?: "UNSURE"),
            FieldSpec("healthNotes", "معلومات صحية إضافية", required = false, defaultValue = sectionData?.get("healthNotes")?.jsonPrimitive?.contentOrNull.orEmpty()),
        )
    }
    val values = remember(area, fields) { mutableStateMapOf<String, String>().apply { fields.forEach { put(it.key, it.defaultValue) } } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            areas.forEach { (key, label) -> FilterChip(selected = area == key, onClick = { area = key }, label = { Text(label) }, modifier = Modifier.testTag("profile-section-$key")) }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEach { field ->
                    if (field.options.isNotEmpty()) ChoiceField(field, values[field.key].orEmpty(), safeProfile = true, onChange = { values[field.key] = it })
                    else OutlinedTextField(values[field.key].orEmpty(), { input -> values[field.key] = if (field.numeric) input.filter { it.isDigit() } else input }, modifier = Modifier.fillMaxWidth(), label = { Text(field.label) }, singleLine = true)
                }
                Button(onClick = {
                    val route = if (area == "profile" || area == "preferences") "profile" else "profile:$area"
                    val body = if (area == "conditions") buildConditionsBody(values) else buildBody(fields, values)
                    onSave(route, body)
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("profile-save")) { Text(if (busy) "جارٍ الحفظ…" else "حفظ التعديلات") }
            }
        }
        Text("ترسل هذه الشاشة الحقول المتوافقة مع تحقق الخادم فقط.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    }
}

private val healthConditionTypes = listOf("HYPERTENSION", "HEART_DISEASE", "KIDNEY_DISEASE", "HIGH_CHOLESTEROL", "RETINOPATHY", "NEUROPATHY", "FOOT_PROBLEMS", "OTHER")

private fun conditionFields(root: JsonObject): List<FieldSpec> {
    val raw = root["conditions"]
    val list = when (raw) {
        is JsonArray -> raw
        is JsonObject -> raw["items"] as? JsonArray
        else -> null
    }
    val old = list.orEmpty().mapNotNull { element ->
        runCatching { element.jsonObject }.getOrNull()?.let { item ->
            val type = item["conditionType"]?.jsonPrimitive?.contentOrNull
            val status = item["status"]?.jsonPrimitive?.contentOrNull
            val notes = item["notes"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (type != null && status != null) type to (status to notes) else null
        }
    }.toMap()
    val types = (healthConditionTypes + old.keys.filterNot(healthConditionTypes::contains)).distinct()
    return types.flatMap { type ->
        val conditionName = conditionTypeLabel(type)
        val status = old[type]?.first ?: "UNKNOWN"
        val notes = old[type]?.second.orEmpty()
        listOf(
            FieldSpec("status:$type", "$conditionName — الحالة", listOf("DIAGNOSED", "NOT_DIAGNOSED", "UNKNOWN"), required = false, defaultValue = status),
            FieldSpec("notes:$type", "$conditionName — ملاحظات اختيارية", required = false, defaultValue = notes),
        )
    }
}

internal fun buildConditionsBody(values: Map<String, String>): JsonObject = buildJsonObject {
    val includedTypes = (healthConditionTypes + values.keys.mapNotNull { key -> key.removePrefix("status:").takeIf { key.startsWith("status:") } }).distinct()
    put("conditions", JsonArray(includedTypes.map { type ->
        buildJsonObject {
            put("conditionType", type)
            put("status", values["status:$type"].orEmpty().ifBlank { "UNKNOWN" })
            values["notes:$type"]?.trim()?.takeIf(String::isNotEmpty)?.let { put("notes", it) }
        }
    }))
}

@Composable
private fun RecordList(
    endpoint: String,
    data: JsonObject?,
    onDelete: (String, String) -> Unit,
    onEdit: ((String, JsonObject) -> Unit)? = null,
    timeZoneId: String? = null,
    numeralSystem: String? = null,
) {
    val items = (data?.get("items") as? JsonArray).orEmpty()
    if (items.isEmpty()) {
        Text(emptyLabel(endpoint), Modifier.fillMaxWidth().padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        return
    }
    items.forEach { element ->
        val record = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
        val id = if (endpoint == "administrations") {
            record["administration"]?.let { runCatching { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull() }
        } else record["id"]?.jsonPrimitive?.contentOrNull
        var confirmDelete by remember(id) { mutableStateOf(false) }
        if (confirmDelete && id != null) AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف السجل؟") },
            text = { Text("سيُحذف هذا السجل من حسابك.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(endpoint, id) }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.End) {
                MedicalValueText(recordTitle(endpoint, record), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                recordPresentationFields(endpoint, record, timeZoneId, numeralSystem).forEach { field ->
                        MedicalLabelValueRow(
                            label = field.label,
                            value = field.value,
                            labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            valueStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                }
                if (id != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onEdit != null && endpoint in setOf("glucose", "medications")) OutlinedButton(onClick = { onEdit(id, record) }) { Text("تعديل") }
                        OutlinedButton(onClick = { confirmDelete = true }) { Text("حذف السجل") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordForm(endpoint: String, busy: Boolean, initial: JsonObject? = null, timeZoneId: String? = null, onSave: (JsonObject) -> Unit) {
    val specs = fieldsFor(endpoint, timeZoneId)
    val values = remember(endpoint, initial) { mutableStateMapOf<String, String>().apply { specs.forEach { spec ->
        put(spec.key, recordFormInitialValue(spec.key, initial, spec.defaultValue, timeZoneId))
    } } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            specs.forEach { spec ->
                if (spec.options.isNotEmpty()) ChoiceField(spec, values[spec.key] ?: spec.defaultValue, onChange = { values[spec.key] = it })
                else {
                    val value = values[spec.key].orEmpty()
                    val invalidNumber = spec.numeric && value.isNotBlank() && !isValidMedicalNumeric(value)
                    OutlinedTextField(
                        value = value, onValueChange = { updated -> values[spec.key] = if (spec.numeric) updated.filter { it in '0'..'9' || it == '.' } else updated },
                        label = { Text(spec.label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (spec.numeric) KeyboardType.Decimal else KeyboardType.Text),
                        textStyle = TextStyle.Default.copy(textDirection = if (spec.numeric) TextDirection.Ltr else TextDirection.ContentOrRtl),
                        isError = invalidNumber,
                        supportingText = if (invalidNumber) ({ Text("أدخل رقمًا صحيحًا.") }) else null,
                    )
                }
            }
            val requiredComplete = specs.filter(FieldSpec::required).all { values[it.key].orEmpty().isNotBlank() }
            val numericValuesValid = specs.filter(FieldSpec::numeric).all { isValidMedicalNumeric(values[it.key]) }
            Button(onClick = { onSave(buildBody(specs, values)) }, enabled = !busy && requiredComplete && numericValuesValid, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "جارٍ الحفظ…" else "حفظ السجل")
            }
        }
    }
}

@Composable
private fun ChoiceField(spec: FieldSpec, selected: String, safeProfile: Boolean = false, onChange: (String) -> Unit) {
    var expanded by remember(spec.key) { mutableStateOf(false) }
    val options = (spec.options + selected.takeIf { it.isNotBlank() && it !in spec.options }.orEmpty()).distinct()
    fun label(value: String): String = if (safeProfile) {
        val kind = when {
            spec.key.startsWith("status:") -> "conditionStatus"
            else -> spec.key
        }
        profileEnumLabel(kind, value)
    } else enumLabel(value)
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("${spec.label}: ${label(selected)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(label(option)) }, onClick = { onChange(option); expanded = false }) }
        }
    }
}

@Composable
private fun AdministrationForm(medications: JsonObject?, busy: Boolean, onSave: (JsonObject) -> Unit) {
    val items = (medications?.get("items") as? JsonArray).orEmpty()
    val options = items.mapNotNull { element -> runCatching { element.jsonObject }.getOrNull()?.let { record ->
        val id = record["id"]?.jsonPrimitive?.contentOrNull ?: return@let null
        id to (record["medicationName"]?.jsonPrimitive?.contentOrNull ?: "دواء")
    } }
    var selected by remember(options) { mutableStateOf(options.firstOrNull()?.first.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("UNIT") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString().take(5)) }
    var status by remember { mutableStateOf("TAKEN") }
    var notes by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }, enabled = options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(options.firstOrNull { it.first == selected }?.second ?: "اختر دواءً مسجلاً") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { selected = id; expanded = false }) } }
            }
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), label = { Text("الجرعة الفعلية (اختياري)") }, singleLine = true)
            ChoiceField(FieldSpec("doseUnit", "الوحدة", doseUnits, required = false, defaultValue = "UNIT"), unit) { unit = it }
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("التاريخ") }, singleLine = true)
            OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(), label = { Text("الوقت") }, singleLine = true)
            ChoiceField(FieldSpec("status", "الحالة", listOf("TAKEN", "MISSED", "DELAYED"), defaultValue = "TAKEN"), status) { status = it }
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("ملاحظات (اختياري)") }, singleLine = true)
            Button(onClick = {
                val body = buildJsonObject {
                    put("medicationId", selected); put("administrationDate", date); put("administrationTime", time); put("status", status)
                    amount.toDoubleOrNull()?.let { put("actualDose", JsonPrimitive(it)) }
                    if (amount.isNotBlank()) put("doseUnit", unit)
                    if (notes.isNotBlank()) put("notes", notes)
                }
                onSave(body)
            }, enabled = !busy && selected.isNotBlank() && date.isNotBlank() && time.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("حفظ الجرعة") }
        }
    }
}

private fun fieldsFor(endpoint: String, timeZoneId: String? = null): List<FieldSpec> {
    val zone = runCatching { ZoneId.of(timeZoneId ?: "Asia/Baghdad") }.getOrElse { ZoneId.of("Asia/Baghdad") }
    val date = LocalDate.now(zone).toString()
    val time = LocalTime.now(zone).withSecond(0).withNano(0).toString().take(5)
    val notes = FieldSpec("notes", "ملاحظات", required = false)
    return when (endpoint) {
        "glucose" -> listOf(FieldSpec("glucoseValue", "قيمة السكر", numeric = true), FieldSpec("glucoseUnit", "الوحدة", listOf("MG_DL", "MMOL_L"), defaultValue = "MG_DL"), FieldSpec("measurementDate", "التاريخ", defaultValue = date), FieldSpec("measurementTime", "الوقت", defaultValue = time), FieldSpec("measurementContext", "السياق", glucoseContexts, defaultValue = "FASTING"), FieldSpec("source", "مصدر القياس", listOf("FINGERSTICK_METER", "CGM", "LAB", "OTHER", "NOT_SPECIFIED"), defaultValue = "NOT_SPECIFIED"), FieldSpec("symptoms", "الأعراض", required = false), notes)
        "medications" -> listOf(FieldSpec("medicationName", "اسم الدواء"), FieldSpec("category", "الفئة", listOf("INSULIN", "BIGUANIDE", "SULFONYLUREA", "DPP4_INHIBITOR", "SGLT2_INHIBITOR", "GLP1_AGONIST", "TZD", "MEGLITINIDE", "ALPHA_GLUCOSIDASE_INHIBITOR", "ANTIHYPERTENSIVE", "STATIN", "OTHER"), defaultValue = "OTHER"), FieldSpec("route", "طريقة الاستخدام", listOf("SUBCUTANEOUS", "INTRAVENOUS", "INTRAMUSCULAR", "ORAL", "INHALED", "TOPICAL", "OTHER"), defaultValue = "ORAL"), FieldSpec("frequency", "التكرار", listOf("ONCE_DAILY", "TWICE_DAILY", "THREE_TIMES_DAILY", "FOUR_TIMES_DAILY", "WITH_MEALS", "BEFORE_MEALS", "AT_BEDTIME", "AS_NEEDED", "WEEKLY", "OTHER"), defaultValue = "ONCE_DAILY"), FieldSpec("prescribedDose", "الجرعة الموصوفة (اختياري)", numeric = true, required = false), FieldSpec("doseUnit", "وحدة الجرعة", doseUnits, required = false), FieldSpec("startDate", "تاريخ البدء", required = false, defaultValue = date), notes)
        "meals" -> listOf(FieldSpec("mealType", "نوع الوجبة", listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK", "OTHER"), defaultValue = "BREAKFAST"), FieldSpec("description", "وصف الوجبة", required = false), FieldSpec("mealDate", "التاريخ", defaultValue = date), FieldSpec("mealTime", "الوقت", defaultValue = time), FieldSpec("carbohydrateGrams", "الكربوهيدرات (غرام، اختياري)", numeric = true, required = false), notes)
        "activities" -> listOf(FieldSpec("activityType", "نوع النشاط"), FieldSpec("durationMinutes", "المدة بالدقائق", numeric = true), FieldSpec("intensity", "الشدة", listOf("LIGHT", "MODERATE", "VIGOROUS"), defaultValue = "MODERATE"), FieldSpec("activityDate", "التاريخ", defaultValue = date), FieldSpec("activityTime", "الوقت", defaultValue = time), notes)
        "sleep" -> listOf(FieldSpec("durationMinutes", "مدة النوم بالدقائق", numeric = true), FieldSpec("quality", "جودة النوم", listOf("GOOD", "FAIR", "POOR", "NOT_SPECIFIED"), defaultValue = "NOT_SPECIFIED"), FieldSpec("sleepDate", "التاريخ", defaultValue = date), notes)
        "symptoms" -> listOf(FieldSpec("symptomType", "العَرَض", listOf("DIZZINESS", "SWEATING", "TREMOR", "EXCESSIVE_THIRST", "FATIGUE", "HEADACHE", "BLURRED_VISION", "PALPITATIONS", "NAUSEA", "OTHER"), defaultValue = "OTHER"), FieldSpec("severity", "الشدة", listOf("MILD", "MODERATE", "SEVERE"), defaultValue = "MILD"), FieldSpec("symptomDate", "التاريخ", defaultValue = date), FieldSpec("symptomTime", "الوقت", defaultValue = time), notes)
        "factors" -> listOf(FieldSpec("factorType", "العامل", listOf("ILLNESS", "STRESS", "TRAVEL", "MENSTRUATION", "OTHER"), defaultValue = "OTHER"), FieldSpec("description", "الوصف", required = false), FieldSpec("factorDate", "التاريخ", defaultValue = date), FieldSpec("factorTime", "الوقت", required = false, defaultValue = time))
        "weight" -> listOf(FieldSpec("weightKg", "الوزن بالكيلوغرام", numeric = true), FieldSpec("measurementDate", "التاريخ", defaultValue = date), FieldSpec("notes", "ملاحظات", required = false))
        "labs" -> listOf(FieldSpec("testType", "نوع التحليل", listOf("HBA1C", "CREATININE", "EGFR", "URINE_ACR", "BLOOD_PRESSURE", "TOTAL_CHOLESTEROL", "LDL", "HDL", "TRIGLYCERIDES", "OTHER"), defaultValue = "HBA1C"), FieldSpec("value", "النتيجة", numeric = true), FieldSpec("valueSecondary", "قيمة إضافية (اختياري)", numeric = true, required = false), FieldSpec("unit", "الوحدة"), FieldSpec("testDate", "تاريخ التحليل", defaultValue = date), FieldSpec("referenceRange", "المدى المرجعي (اختياري)", required = false), notes)
        else -> emptyList()
    }
}

private fun buildBody(specs: List<FieldSpec>, values: Map<String, String>): JsonObject = buildJsonObject {
    specs.forEach { spec ->
        val value = values[spec.key]?.trim().orEmpty()
        if (value.isNotEmpty()) put(spec.key, if (spec.numeric && spec.key in setOf("durationMinutes", "diagnosisYear")) JsonPrimitive(value.toIntOrNull() ?: 0) else if (spec.numeric) JsonPrimitive(value.toDoubleOrNull() ?: 0.0) else JsonPrimitive(value))
    }
}

private fun recordTitle(endpoint: String, record: JsonObject): String {
    if (endpoint == "glucose") return "قراءة السكر"
    if (endpoint == "administrations") return "جرعة مسجلة"
    if (endpoint == "medications") {
        return record["medicationName"]?.jsonPrimitive?.contentOrNull
            ?: record["medication"]?.let { runCatching { it.jsonObject["medicationName"]?.jsonPrimitive?.contentOrNull }.getOrNull() }
            ?: "دواء"
    }
    val keys = when (endpoint) {
        "meals" -> listOf("mealType", "description")
        "activities" -> listOf("activityType")
        "sleep" -> listOf("durationMinutes")
        "symptoms" -> listOf("symptomType")
        "weight" -> listOf("weightKg")
        else -> listOf("testType")
    }
    return keys.mapNotNull { key ->
        val raw = record[key]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        if (key == "glucoseValue") {
            MedicalPresentation.formatGlucose(raw, record["glucoseUnit"]?.jsonPrimitive?.contentOrNull)
        } else {
            MedicalPresentation.presentRecordField(key, raw)?.value
        }
    }.joinToString(" — ").ifBlank { "سجل" }
}

/** Ordered patient-facing fields for glucose, medication, and administration cards. */
internal fun recordPresentationFields(
    endpoint: String,
    record: JsonObject,
    timeZoneId: String? = null,
    numeralSystem: String? = null,
): List<MedicalFieldPresentation> {
    val recordData = if (endpoint == "administrations") {
        record["administration"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: record
    } else record
    val orderedKeys = when (endpoint) {
        "glucose" -> listOf("glucoseValue", "measurementContext", "measurementDatetime", "source", "minutesAfterMeal", "symptomsText", "notes")
        "medications" -> listOf("category", "status", "strength", "prescribedDose", "route", "frequency", "usualTime", "startDate", "endDate", "prescriberName", "insulinFormulation", "notes")
        "administrations" -> listOf("actualDose", "administrationDatetime", "status", "notes")
        else -> emptyList()
    }
    val hiddenOrCombined = setOf("glucoseUnit", "doseUnit", "strengthUnit", "standardizedValueMgdl") + if (endpoint == "medications") setOf("medicationName") else emptySet()
    val result = mutableListOf<MedicalFieldPresentation>()
    fun scalar(element: JsonElement?): String? = element?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    fun append(key: String, element: JsonElement?, doseUnit: String? = null, strengthUnit: String? = null) {
        MedicalPresentation.presentRecordField(
            key = key,
            value = scalar(element),
            timeZoneId = timeZoneId,
            numeralSystem = numeralSystem,
            glucoseUnit = scalar(recordData["glucoseUnit"]),
            doseUnit = doseUnit,
            strengthUnit = strengthUnit,
        )?.let(result::add)
    }

    if (endpoint == "administrations") {
        val medicationName = record["medication"]?.let {
            runCatching { it.jsonObject["medicationName"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        }
        medicationName?.takeIf(String::isNotBlank)?.let { result += MedicalFieldPresentation("الدواء", it) }
    }
    orderedKeys.forEach { key ->
        val unit = scalar(recordData["doseUnit"])
        val strengthUnit = scalar(recordData["strengthUnit"])
        append(key, recordData[key], doseUnit = unit, strengthUnit = strengthUnit)
    }
    recordData.keys.filter { it !in orderedKeys && it !in hiddenOrCombined }.forEach { key ->
        if (key == "medication") return@forEach
        append(key, recordData[key])
    }
    return result
}

/** Restores the form's date/time controls from the API's timestamp without changing its instant. */
internal fun recordFormInitialValue(key: String, initial: JsonObject?, defaultValue: String, timeZoneId: String? = null): String {
    val direct = initial?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    if (!direct.isNullOrBlank()) return direct
    if (initial == null) return defaultValue
    if (key == "symptoms") {
        return initial["symptomsText"]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }.orEmpty()
    }
    if (key !in setOf("measurementDate", "measurementTime")) return defaultValue
    val raw = initial["measurementDatetime"]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
        ?: return defaultValue
    val instant = runCatching { Instant.parse(raw) }.getOrElse {
        runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
    } ?: return ""
    val zone = runCatching { ZoneId.of(timeZoneId ?: "Asia/Baghdad") }.getOrElse { ZoneId.of("Asia/Baghdad") }
    val local = instant.atZone(zone)
    return if (key == "measurementDate") DateTimeFormatter.ISO_LOCAL_DATE.format(local) else DateTimeFormatter.ofPattern("HH:mm").format(local)
}

internal fun isValidMedicalNumeric(value: String?): Boolean = value.isNullOrBlank() || value.trim().toBigDecimalOrNull() != null

private fun emptyLabel(endpoint: String) = when (endpoint) {
    "glucose" -> "لم تسجل أي قياس بعد."; "medications" -> "لا توجد أدوية مسجلة."; "administrations" -> "لا توجد جرعات مسجلة."
    "meals" -> "لا توجد وجبات مسجلة."; "activities" -> "لا توجد أنشطة مسجلة."; "sleep" -> "لا توجد سجلات نوم."; "symptoms" -> "لا توجد أعراض مسجلة."; "factors" -> "لا توجد عوامل يومية مسجلة."; "weight" -> "لا توجد قياسات وزن."; else -> "لا توجد نتائج تحاليل مسجلة."
}

private fun enumLabel(value: String) = MedicalPresentation.genericEnumLabel(value)
