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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import dev.techda.sahatisukari.BuildConfig
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.json.JsonObject

@Composable
fun SettingsScreen(onBack: () -> Unit, onSignedOut: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var sessionConfirmation by remember { mutableStateOf(SessionConfirmationState()) }
    var email by remember(state.email) { mutableStateOf(state.email) }
    var phone by remember(state.phone) { mutableStateOf(state.phone) }
    LaunchedEffect(Unit) { viewModel.loadInitial() }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SahatiPageTitle("الإعدادات", fontWeight = FontWeight.Normal)
        Text("تحديثات التطبيق", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
        Text("الإصدار المثبت: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        state.updateCheckedAt?.let { Text("آخر تحقق ناجح: $it", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start) }
        state.updateCheck?.let { result ->
            Text(
                if (result.available) "يتوفر تحديث جديد: ${result.availableVersionName} (${result.availableVersionCode})."
                else "أنت تستخدم أحدث إصدار من التطبيق.",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            if (result.available && result.required) Text("هذا التحديث مطلوب للاستمرار في استخدام الإصدار المدعوم.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start)
            if (result.available && result.manifest.releaseNotes.isNotBlank()) Text(result.manifest.releaseNotes, Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            if (result.available) UpdateInstallButton(result)
        }
        state.updateError?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
        if (state.updateBusy) {
            Text("جارٍ التحقق من التحديث…", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            CircularProgressIndicator()
            OutlinedButton(onClick = viewModel::cancelUpdateCheck, modifier = Modifier.fillMaxWidth()) { Text("إلغاء") }
        }
        OutlinedButton(onClick = { viewModel.checkForUpdates(context) }, enabled = !state.updateBusy, modifier = Modifier.fillMaxWidth()) { Text(if (state.updateBusy) "جارٍ التحقق…" else "التحقق من وجود تحديث") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("العودة") }
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
        state.notice?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start) }
        Text("معلومات الحساب", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true)
        Button(onClick = { viewModel.saveContact(email, phone) }, enabled = !state.busy && (email != state.email || phone != state.phone), modifier = Modifier.fillMaxWidth()) { Text("حفظ معلومات الحساب") }
        Text("تفضيلات العرض", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
        SettingsChoice("وحدة قياس السكر", state.glucoseUnit, listOf("MG_DL", "MMOL_L"), viewModel::savePreferences, state.numerals, true)
        SettingsChoice("الأرقام", state.numerals, listOf("LATIN", "ARABIC_INDIC"), viewModel::savePreferences, state.glucoseUnit, false)
        Text("تغيير كلمة المرور", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
        SecretField("كلمة المرور الحالية", current) { current = it }
        SecretField("كلمة المرور الجديدة", next) { next = it }
        SecretField("تأكيد كلمة المرور الجديدة", confirm) { confirm = it }
        Text("10 أحرف على الأقل، وحرف إنجليزي كبير وصغير ورقم.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        Button(onClick = { viewModel.changePassword(current, next, confirm) }, enabled = !state.busy && current.isNotEmpty() && next.isNotEmpty() && confirm.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("تحديث كلمة المرور") }
        Text("الأجهزة النشطة", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
        SessionCards(
            state.sessions,
            state.timeZoneId,
            state.numerals,
            busy = state.busy,
            onRevoke = { session -> sessionConfirmation = sessionConfirmation.askToRevoke(session) },
        )
        OutlinedButton(onClick = viewModel::loadSessions, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تحديث الجلسات") }
        Button(onClick = { sessionConfirmation = sessionConfirmation.askToRevokeAll() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تسجيل الخروج من جميع الأجهزة") }
        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
    sessionConfirmation.selectedSession?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionConfirmation = sessionConfirmation.cancel() },
            title = { Text("تأكيد إنهاء الجلسة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(session.deviceName, fontWeight = FontWeight.SemiBold)
                    Text(session.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("تاريخ إنشاء الجلسة: ${session.createdAt}")
                    Text("آخر نشاط: ${session.lastActivity}")
                    Text("سيُمنع هذا الجهاز من الوصول إلى الحساب. لا يحدد الخادم الجلسة المستخدمة حاليًا؛ إذا أنهيت جلسة هذا التطبيق فستعود إلى شاشة تسجيل الدخول.")
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = {
                    val id = sessionConfirmation.confirmedSessionId()
                    sessionConfirmation = sessionConfirmation.cancel()
                    id?.let(viewModel::revokeSession)
                }) { Text("إنهاء الجلسة") }
            },
            dismissButton = { TextButton(onClick = { sessionConfirmation = sessionConfirmation.cancel() }) { Text("إلغاء") } },
        )
    }
    if (sessionConfirmation.revokeAllPending) {
        AlertDialog(
            onDismissRequest = { sessionConfirmation = sessionConfirmation.cancel() },
            title = { Text("تسجيل الخروج من جميع الأجهزة؟") },
            text = { Text("سيُنهي هذا الإجراء جلسات التطبيق والموقع، بما فيها هذه الجلسة. ستحتاج إلى تسجيل الدخول مجددًا على هذا الجهاز.") },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = {
                    val confirmed = sessionConfirmation.confirmedRevokeAll()
                    sessionConfirmation = sessionConfirmation.cancel()
                    if (confirmed) viewModel.logoutAll()
                }) { Text("تسجيل الخروج من الجميع") }
            },
            dismissButton = { TextButton(onClick = { sessionConfirmation = sessionConfirmation.cancel() }) { Text("إلغاء") } },
        )
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
private fun SessionCards(
    data: JsonObject?,
    timeZoneId: String?,
    numeralSystem: String?,
    busy: Boolean,
    onRevoke: (ActiveSessionCard) -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SessionCardsContent(data, timeZoneId, numeralSystem, busy, onRevoke)
    }
}

@Composable
private fun SessionCardsContent(
    data: JsonObject?,
    timeZoneId: String?,
    numeralSystem: String?,
    busy: Boolean,
    onRevoke: (ActiveSessionCard) -> Unit,
) {
    if (data == null) {
        Text("جارٍ تحميل الجلسات…", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        return
    }
    val sessions = activeSessionCards(data, timeZoneId, numeralSystem)
    if (sessions.isEmpty()) {
        Text("لا توجد جلسات مسجلة.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        return
    }
    sessions.forEach { session ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                Text(session.deviceName, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
                Text(session.description, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
                Text("تاريخ إنشاء الجلسة", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Start)
                MedicalValueText(session.createdAt, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
                Text("آخر نشاط", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Start)
                MedicalValueText(session.lastActivity, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { onRevoke(session) }, enabled = !busy) { Text("إنهاء هذه الجلسة") }
            }
        }
    }
}

private fun choiceLabel(value: String) = when (value) { "MG_DL" -> "ملغم/دل"; "MMOL_L" -> "مليمول/لتر"; "LATIN" -> "أرقام لاتينية (123)"; "ARABIC_INDIC" -> "أرقام عربية (١٢٣)"; else -> value }
