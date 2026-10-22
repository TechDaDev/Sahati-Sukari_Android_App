package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import dev.techda.sahatisukari.BuildConfig
import dev.techda.sahatisukari.data.UpdateManifestValidator
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun SettingsScreen(onBack: () -> Unit, onSignedOut: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var email by remember(state.email) { mutableStateOf(state.email) }
    var phone by remember(state.phone) { mutableStateOf(state.phone) }
    LaunchedEffect(Unit) { viewModel.loadInitial() }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("الإعدادات", Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.End)
        Text("تحديثات التطبيق", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        Text("الإصدار المثبت: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        state.updateCheckedAt?.let { Text("آخر تحقق ناجح: $it", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End) }
        state.updateCheck?.let { result ->
            Text(if (result.available) "يتوفر إصدار جديد: ${result.manifest.versionName}" else "أنت تستخدم أحدث إصدار من التطبيق.", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            if (result.available) Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManifestValidator.APK_URL))) }, modifier = Modifier.fillMaxWidth()) { Text("تحديث الآن") }
        }
        state.updateError?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End) }
        OutlinedButton(onClick = { viewModel.checkForUpdates(BuildConfig.VERSION_CODE) }, enabled = !state.updateBusy, modifier = Modifier.fillMaxWidth()) { Text(if (state.updateBusy) "جارٍ التحقق…" else "التحقق من وجود تحديث") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("العودة") }
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End) }
        state.notice?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End) }
        Text("معلومات الحساب", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true)
        Button(onClick = { viewModel.saveContact(email, phone) }, enabled = !state.busy && (email != state.email || phone != state.phone), modifier = Modifier.fillMaxWidth()) { Text("حفظ معلومات الحساب") }
        Text("تفضيلات العرض", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        SettingsChoice("وحدة قياس السكر", state.glucoseUnit, listOf("MG_DL", "MMOL_L"), viewModel::savePreferences, state.numerals, true)
        SettingsChoice("الأرقام", state.numerals, listOf("LATIN", "ARABIC_INDIC"), viewModel::savePreferences, state.glucoseUnit, false)
        Text("تغيير كلمة المرور", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        SecretField("كلمة المرور الحالية", current) { current = it }
        SecretField("كلمة المرور الجديدة", next) { next = it }
        SecretField("تأكيد كلمة المرور الجديدة", confirm) { confirm = it }
        Text("10 أحرف على الأقل، وحرف إنجليزي كبير وصغير ورقم.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        Button(onClick = { viewModel.changePassword(current, next, confirm) }, enabled = !state.busy && current.isNotEmpty() && next.isNotEmpty() && confirm.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("تحديث كلمة المرور") }
        Text("الأجهزة النشطة", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        SessionCards(state.sessions, onDelete = viewModel::revokeSession)
        OutlinedButton(onClick = viewModel::loadSessions, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تحديث الجلسات") }
        Button(onClick = viewModel::logoutAll, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تسجيل الخروج من جميع الأجهزة") }
        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        if (state.updateBusy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingsChoice(label: String, selected: String, options: List<String>, save: (String, String) -> Unit, other: String, unitChoice: Boolean) {
    var expanded by remember(label) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${choiceLabel(selected)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(choiceLabel(option)) }, onClick = { expanded = false; if (unitChoice) save(option, other) else save(other, option) }) }
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
}

@Composable
private fun SessionCards(data: JsonObject?, onDelete: (String) -> Unit) {
    val array = (data?.get("sessions") ?: data?.get("items")) as? JsonArray
    if (array.isNullOrEmpty()) {
        Text("لا توجد جلسات إضافية لعرضها.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        return
    }
    array.forEach { item ->
        val record = runCatching { item.jsonObject }.getOrNull() ?: return@forEach
        val id = record["id"]?.jsonPrimitive?.content ?: return@forEach
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                Text(record["deviceLabel"]?.jsonPrimitive?.content ?: "جهاز", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                Text(record["lastUsedAt"]?.jsonPrimitive?.content ?: "", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                OutlinedButton(onClick = { onDelete(id) }) { Text("إنهاء الجلسة") }
            }
        }
    }
}

private fun choiceLabel(value: String) = when (value) { "MG_DL" -> "ملغم/دل"; "MMOL_L" -> "مليمول/لتر"; "LATIN" -> "أرقام لاتينية (123)"; "ARABIC_INDIC" -> "أرقام عربية (١٢٣)"; else -> value }
