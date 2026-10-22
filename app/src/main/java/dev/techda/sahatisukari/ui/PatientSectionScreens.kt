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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalTime

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
        Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End) }
        state.notice?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End) }
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
            if (section == 2) Text("سجّل قراءاتك كما ظهرت على الجهاز. لا يقدم التطبيق تفسيرًا طبيًا للقراءات.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            Button(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) { Text(if (adding) "إلغاء" else if (section == 2) "إضافة قياس" else "إضافة دواء") }
            if (adding) RecordForm(endpoint, state.busy) { body -> viewModel.save(endpoint, body); adding = false }
            editing?.let { (id, record) -> RecordForm(endpoint, state.busy, record) { body -> viewModel.save(endpoint, body, id); editing = null } }
            RecordList(endpoint, state.data, onDelete = viewModel::delete, onEdit = { id, record -> editing = id to record })
            PaginationAction(state.data, state.busy, viewModel::loadMore)
            if (section == 3) {
                Text("تسجيل الدواء لا يغيّر الجرعة الموصوفة. سجّل الجرعة الفعلية التي تناولتها فقط.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                Text("سجل الجرعات", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Button(onClick = { addingDose = !addingDose }, modifier = Modifier.fillMaxWidth()) { Text(if (addingDose) "إلغاء" else "تسجيل جرعة") }
                if (addingDose) AdministrationForm(state.data, state.busy) { body -> viewModel.save("administrations", body); addingDose = false }
                RecordList("administrations", state.relatedData, onDelete = viewModel::delete)
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
        Text("يومياتي", Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { (key, title) -> FilterChip(selected = endpoint == key, onClick = { endpoint = key }, label = { Text(title) }) }
        }
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End) }
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
            Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            OutlinedButton(onClick = { setAdding(!adding) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (adding) "إلغاء" else if (endpoint == "weight") "إضافة قياس وزن" else "إضافة تحليل")
            }
            if (adding) RecordForm(endpoint, busy) { body -> onSave(endpoint, body); setAdding(false) }
            if (loading && data == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            else if (items.isEmpty()) Text(if (loading) "جارٍ تحميل السجل…" else "لا توجد سجلات لعرضها.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            records.forEach { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        Text(record.title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                        record.fields.forEach { field ->
                            Text("${field.label}: ${field.value}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
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
        Text("ترسل هذه الشاشة الحقول المتوافقة مع تحقق الخادم فقط.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
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
private fun RecordList(endpoint: String, data: JsonObject?, onDelete: (String, String) -> Unit, onEdit: ((String, JsonObject) -> Unit)? = null) {
    val items = (data?.get("items") as? JsonArray).orEmpty()
    if (items.isEmpty()) {
        Text(emptyLabel(endpoint), Modifier.fillMaxWidth().padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        return
    }
    items.forEach { element ->
        val record = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
        val id = record["id"]?.jsonPrimitive?.contentOrNull
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
                Text(recordTitle(endpoint, record), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                record.entries.filter { it.key !in setOf("id", "patientId", "createdAt", "updatedAt") && it.value !is JsonObject && it.value !is JsonArray }.take(5).forEach { (key, value) ->
                    Text("${profileLabel(key)}: ${value.jsonPrimitive.contentOrNull ?: "—"}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
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
private fun RecordForm(endpoint: String, busy: Boolean, initial: JsonObject? = null, onSave: (JsonObject) -> Unit) {
    val specs = fieldsFor(endpoint)
    val values = remember(endpoint, initial) { mutableStateMapOf<String, String>().apply { specs.forEach { spec ->
        val current = initial?.get(spec.key)?.jsonPrimitive?.contentOrNull
        put(spec.key, current ?: spec.defaultValue)
    } } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            specs.forEach { spec ->
                if (spec.options.isNotEmpty()) ChoiceField(spec, values[spec.key] ?: spec.defaultValue, onChange = { values[spec.key] = it })
                else OutlinedTextField(
                    value = values[spec.key].orEmpty(), onValueChange = { value -> values[spec.key] = if (spec.numeric) value.filter { it.isDigit() || it == '.' } else value },
                    label = { Text(spec.label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            Button(onClick = { onSave(buildBody(specs, values)) }, enabled = !busy && specs.filter(FieldSpec::required).all { values[it.key].orEmpty().isNotBlank() }, modifier = Modifier.fillMaxWidth()) {
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

private fun fieldsFor(endpoint: String): List<FieldSpec> {
    val date = LocalDate.now().toString()
    val time = LocalTime.now().withSecond(0).withNano(0).toString().take(5)
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
    val keys = when (endpoint) {
        "glucose" -> listOf("glucoseValue", "glucoseUnit")
        "medications" -> listOf("medicationName")
        "meals" -> listOf("mealType", "description")
        "activities" -> listOf("activityType")
        "sleep" -> listOf("durationMinutes")
        "symptoms" -> listOf("symptomType")
        "weight" -> listOf("weightKg")
        else -> listOf("testType")
    }
    return keys.mapNotNull { key -> record[key]?.jsonPrimitive?.contentOrNull?.let(::enumLabel) }.joinToString(" — ").ifBlank { "سجل" }
}

private fun emptyLabel(endpoint: String) = when (endpoint) {
    "glucose" -> "لم تسجل أي قياس بعد."; "medications" -> "لا توجد أدوية مسجلة."; "administrations" -> "لا توجد جرعات مسجلة."
    "meals" -> "لا توجد وجبات مسجلة."; "activities" -> "لا توجد أنشطة مسجلة."; "sleep" -> "لا توجد سجلات نوم."; "symptoms" -> "لا توجد أعراض مسجلة."; "factors" -> "لا توجد عوامل يومية مسجلة."; "weight" -> "لا توجد قياسات وزن."; else -> "لا توجد نتائج تحاليل مسجلة."
}

private fun profileLabel(key: String) = when (key) {
    "fullName" -> "الاسم"; "email" -> "البريد الإلكتروني"; "phone" -> "الهاتف"; "gender" -> "الجنس"; "dateOfBirth" -> "تاريخ الميلاد"; "heightCm" -> "الطول"; "timezone" -> "المنطقة الزمنية"; "diabetesType" -> "نوع السكري"; "diagnosisDate" -> "تاريخ التشخيص"; "diagnosisYear" -> "سنة التشخيص"; "monitoringMethod" -> "طريقة المتابعة"; "treatmentApproach" -> "أسلوب العلاج"; "preferredGlucoseUnit" -> "وحدة السكر المفضلة"; "numeralSystem" -> "نظام الأرقام"; "testType" -> "نوع التحليل"; "value" -> "النتيجة"; "valueSecondary" -> "قيمة إضافية"; "unit" -> "الوحدة"; "testDate" -> "تاريخ التحليل"; "measurementDate" -> "تاريخ القياس"; "weightKg" -> "الوزن بالكيلوغرام"; "notes" -> "ملاحظات"; "referenceRange" -> "المدى المرجعي"; else -> key
}

private fun enumLabel(value: String) = when (value) {
    "MG_DL" -> "ملغم/دل"; "MMOL_L" -> "مليمول/لتر"; "FASTING" -> "صائم"; "BEFORE_BREAKFAST" -> "قبل الإفطار"; "AFTER_BREAKFAST" -> "بعد الإفطار"; "BEFORE_LUNCH" -> "قبل الغداء"; "AFTER_LUNCH" -> "بعد الغداء"; "BEFORE_DINNER" -> "قبل العشاء"; "AFTER_DINNER" -> "بعد العشاء"; "BEFORE_SLEEP" -> "قبل النوم"; "FINGERSTICK_METER" -> "جهاز قياس"; "CGM" -> "مستشعر مستمر"; "LAB" -> "مختبر"; "NOT_SPECIFIED" -> "غير محدد"; "BREAKFAST" -> "الإفطار"; "LUNCH" -> "الغداء"; "DINNER" -> "العشاء"; "SNACK" -> "وجبة خفيفة"; "LIGHT" -> "خفيف"; "MODERATE" -> "متوسط"; "VIGOROUS" -> "شديد"; "GOOD" -> "جيد"; "FAIR" -> "متوسط"; "POOR" -> "ضعيف"; "DIZZINESS" -> "دوخة"; "SWEATING" -> "تعرق"; "TREMOR" -> "رجفة"; "EXCESSIVE_THIRST" -> "عطش شديد"; "FATIGUE" -> "إرهاق"; "HEADACHE" -> "صداع"; "BLURRED_VISION" -> "تشوش الرؤية"; "PALPITATIONS" -> "خفقان"; "NAUSEA" -> "غثيان"; "MILD" -> "خفيف"; "SEVERE" -> "شديد"; "ILLNESS" -> "مرض"; "STRESS" -> "توتر"; "TRAVEL" -> "سفر"; "MENSTRUATION" -> "الدورة الشهرية"; "INSULIN" -> "إنسولين"; "BIGUANIDE" -> "بيغوانيد"; "OTHER" -> "أخرى"; "ORAL" -> "عن طريق الفم"; "SUBCUTANEOUS" -> "تحت الجلد"; "ONCE_DAILY" -> "مرة يوميًا"; "TWICE_DAILY" -> "مرتين يوميًا"; "THREE_TIMES_DAILY" -> "ثلاث مرات يوميًا"; "FOUR_TIMES_DAILY" -> "أربع مرات يوميًا"; "WITH_MEALS" -> "مع الوجبات"; "BEFORE_MEALS" -> "قبل الوجبات"; "AT_BEDTIME" -> "قبل النوم"; "AS_NEEDED" -> "عند الحاجة"; "WEEKLY" -> "أسبوعيًا"; else -> value.replace('_', ' ').lowercase()
}
