package dev.techda.sahatisukari.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dev.techda.sahatisukari.data.AuthUser
import dev.techda.sahatisukari.BuildConfig
import dev.techda.sahatisukari.ui.UpdateInstallButton
import dev.techda.sahatisukari.ui.auth.AuthState
import dev.techda.sahatisukari.ui.auth.AuthViewModel
import dev.techda.sahatisukari.ui.auth.LoginScreen
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SahatiRoot(authViewModel: AuthViewModel = hiltViewModel()) {
    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    var publicEmergency by rememberSaveable { mutableStateOf(false) }
    var startupUpdate by remember { mutableStateOf<dev.techda.sahatisukari.data.UpdateCheck?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { dev.techda.sahatisukari.data.UpdateRepository.shared.check(context) }
            .onSuccess {
                if (it.available) {
                    startupUpdate = it
                    val dismissed = context.getSharedPreferences("updates", android.content.Context.MODE_PRIVATE)
                        .getLong("later_version_code", 0L)
                    showUpdateDialog = dismissed != it.availableVersionCode
                }
            }
    }
    when (val current = state) {
        AuthState.Checking -> Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        AuthState.SignedOut -> if (publicEmergency) EmergencyHelp(Modifier.fillMaxSize(), onBack = { publicEmergency = false }) else LoginScreen(viewModel = authViewModel, onEmergency = { publicEmergency = true })
        is AuthState.SignedIn -> PatientApp(current.user, onLogout = authViewModel::logout)
    }
    val update = startupUpdate
    if (showUpdateDialog && update != null) {
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = false
                if (!update.required) {
                    context.getSharedPreferences("updates", android.content.Context.MODE_PRIVATE).edit()
                        .putLong("later_version_code", update.availableVersionCode).apply()
                }
            },
            title = { Text(if (update.required) "يلزم تحديث التطبيق" else "يتوفر تحديث جديد") },
            text = {
                Column {
                    Text("الإصدار المثبت: ${BuildConfig.VERSION_NAME} (${update.installedVersionCode})")
                    Text("الإصدار المتاح: ${update.availableVersionName} (${update.availableVersionCode})")
                    if (update.required) Text("هذا التحديث مطلوب للاستمرار في استخدام الإصدار المدعوم.")
                    if (update.manifest.releaseNotes.isNotBlank()) Text(update.manifest.releaseNotes)
                }
            },
            confirmButton = { UpdateInstallButton(update) },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    if (!update.required) {
                        context.getSharedPreferences("updates", android.content.Context.MODE_PRIVATE).edit()
                            .putLong("later_version_code", update.availableVersionCode).apply()
                    }
                }) { Text("لاحقاً") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientApp(user: AuthUser, onLogout: () -> Unit, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Bloodtype, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("صحتي وسكري", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { settings = true; emergency = false }) { Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { emergency = true }) { Icon(Icons.Outlined.Sos, contentDescription = "مساعدة عاجلة", tint = MaterialTheme.colorScheme.error) }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "خروج", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            SahatiBottomNavigation(
                selectedIndex = if (emergency || settings) -1 else selected,
                onSelect = { selected = it; emergency = false; settings = false },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (emergency) EmergencyHelp(Modifier.fillMaxSize().padding(padding))
        else if (settings) SettingsScreen(onBack = { settings = false }, onSignedOut = { settings = false; onLogout() }, modifier = Modifier.fillMaxSize().padding(padding), viewModel = settingsViewModel)
        else if (selected == 0) DashboardScreen(user, Modifier.fillMaxSize().padding(padding), onGlucose = { selected = 2 }, onReports = { selected = 5 }, onProfile = { selected = 1 })
        else PatientSectionScreen(selected, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun DashboardScreen(user: AuthUser, modifier: Modifier = Modifier, onGlucose: () -> Unit, onReports: () -> Unit, onProfile: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = SahatiUiTokens.screenHorizontal, vertical = SahatiUiTokens.screenVertical), verticalArrangement = Arrangement.spacedBy(SahatiUiTokens.sectionSpacing)) {
        Text("لوحة المتابعة الصحية", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        val profile = (state as? DashboardState.Ready)?.data?.obj("profile")
        val patientName = profile?.string("fullName") ?: user.email
        val zone = profile?.string("timezone") ?: "Asia/Baghdad"
        Text("مرحبًا $patientName — اليوم ${todayDate(zone)}", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onGlucose, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 7.dp)) { Text("إضافة قياس سكر") }
            OutlinedButton(onClick = onReports, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 7.dp)) { Text("التقارير") }
        }
        when (val current = state) {
            DashboardState.Loading -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("جارٍ تحميل بياناتك", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            is DashboardState.Failed -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
                }
            }
            is DashboardState.Ready -> DashboardContent(current.data, onProfile)
        }
        Text("هذه اللوحة تعرض السجلات التي أضفتها فقط. لا تقدم تشخيصًا أو توصية علاجية.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    }
}

@Composable
private fun DashboardContent(response: JsonObject, onProfile: () -> Unit) {
    val dash = response.obj("dashboard") ?: return
    val completion = dash.obj("completion")
    val percent = completion?.int("percent")
    if (percent != null && percent < 100) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF8F8)), shape = RoundedCornerShape(SahatiUiTokens.cardRadius)) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                Text("أكمل ملفك الصحي لتحصل على متابعة أدق", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
                Text("نسبة الاكتمال الحالية ${digits(percent)}%", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onProfile) { Text("إكمال الملف الصحي") }
            }
        }
    }
    val latest = dash.obj("latestGlucose")
    StatCard("آخر قراءة سكر", latest?.let { listOfNotNull(it.string("glucoseValue"), it.string("glucoseUnit")).joinToString(" ") } ?: "—", if (latest == null) "لم تسجل أي قراءة بعد" else latest.string("measurementDatetime").orEmpty(), Icons.Outlined.Bloodtype)
    StatCard("قياسات اليوم", dash.string("todayReadings") ?: "—", if (dash.int("todayReadings") == 0) "لم تسجل أي قراءة اليوم" else "قراءات مسجّلة اليوم", Icons.Outlined.Assessment)
    StatCard("آخر جرعة دواء مسجّلة", dash.obj("lastAdministration")?.let { "جرعة مسجّلة" } ?: "—", if (dash.obj("lastAdministration") == null) "لا توجد جرعات مسجّلة" else "", Icons.Outlined.Medication)
    val activityMinutes = dash.string("activityMinutes") ?: "0"
    StatCard("نشاط اليوم", if (activityMinutes == "0") "—" else "$activityMinutes دقيقة", "${dash.array("todayActivities")?.size ?: 0} نشاط مسجّل", Icons.Outlined.CalendarMonth)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("اتجاه قراءات السكر (آخر 14 يومًا)", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            TrendChart(dash)
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Text("ملخص اليوم", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                SummaryRow("الوجبات المسجّلة", dash.string("mealsToday") ?: "—")
                SummaryRow("الأعراض المسجّلة", dash.array("todaySymptoms")?.size?.toString() ?: "—")
                SummaryRow("الوزن الأخير", dash.array("latestWeight")?.firstOrNull()?.jsonObject?.string("weightKg")?.plus(" كغم") ?: "—")
                SummaryRow("آخر HbA1c مسجّل", dash.array("latestHba1c")?.firstOrNull()?.jsonObject?.string("value")?.plus("%") ?: "—")
            }
        }
    }
}

@Composable
private fun TrendChart(dashboard: JsonObject) {
    val points = dashboard.array("trendData").orEmpty().mapNotNull { element ->
        val value = runCatching { element.jsonObject.string("value")?.toFloatOrNull() }.getOrNull() ?: return@mapNotNull null
        value
    }
    if (points.size < 2) {
        Text("لا توجد بيانات كافية لعرض الاتجاه. يلزم وجود قراءتين على الأقل.", Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Text("${points.minOrNull()} — ${points.maxOrNull()}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val horizontalInset = 8.dp.toPx()
        val verticalInset = 8.dp.toPx()
        val chartWidth = size.width - 2 * horizontalInset
        val chartHeight = size.height - 2 * verticalInset
        repeat(4) { index ->
            val y = verticalInset + chartHeight * index / 3f
            drawLine(gridColor, Offset(horizontalInset, y), Offset(size.width - horizontalInset, y), strokeWidth = 1.dp.toPx())
        }
        val min = points.minOrNull() ?: 0f
        val max = points.maxOrNull() ?: min
        val spread = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = horizontalInset + chartWidth * index / (points.size - 1).toFloat()
            val y = verticalInset + chartHeight * (1f - (value - min) / spread)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun StatCard(label: String, value: String, detail: String, icon: ImageVector) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4A90D9), modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            }
        }
    }
}

@Composable
internal fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionScreen(title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("تعذر تحميل هذا القسم الآن. أعد المحاولة لاحقًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmergencyHelp(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (onBack != null) TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى تسجيل الدخول") }
        Text("المساعدة العاجلة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("إذا كانت الحالة طارئة، اطلب المساعدة الطبية فورًا.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("رقم الطوارئ الموحد في العراق", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("911", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))) }, modifier = Modifier.fillMaxWidth()) { Text("الاتصال بالطوارئ 911") }
            }
        }
        Text("لن يجري التطبيق اتصالًا تلقائيًا. زر الاتصال يفتح تطبيق الهاتف.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("لا تراقب المنصة القراءات باستمرار، ولا ترسل تنبيهًا تلقائيًا، ولا تشخّص الأعراض أو تحسب جرعات الدواء.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
    }
}

private fun todayDate(zone: String): String = runCatching { LocalDate.now(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.forLanguageTag("ar-IQ-u-nu-latn"))) }.getOrDefault("")
private fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)
private fun JsonObject.array(key: String): JsonArray? = (this[key] as? JsonArray)
private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()
private fun digits(value: Int) = String.format(Locale.forLanguageTag("ar-IQ-u-nu-latn"), "%d", value)
