package dev.techda.sahatisukari.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.activity.compose.BackHandler
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

@Composable
fun SahatiRoot(authViewModel: AuthViewModel = hiltViewModel()) {
    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    var publicEmergency by rememberSaveable { mutableStateOf(false) }
    var startupUpdate by remember { mutableStateOf<dev.techda.sahatisukari.data.UpdateCheck?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var startupCheckStarted by remember { mutableStateOf(false) }
    var dismissedUpdateVersion by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state) {
        if (dev.techda.sahatisukari.data.UpdateStartupPolicy.shouldCheck(
                authIsChecking = state == AuthState.Checking,
                alreadyCheckedThisLaunch = startupCheckStarted,
            )
        ) {
            startupCheckStarted = true
            withFrameNanos { }
            android.util.Log.i("AppUpdate", "Foreground manifest check started")
            runCatching {
                dev.techda.sahatisukari.data.UpdateRepository.shared.check(context, forceRefresh = true)
            }.onSuccess { result ->
                android.util.Log.i("AppUpdate", "Foreground manifest check completed; updateAvailable=${result.available}")
                if (result.available) {
                    startupUpdate = result
                    showUpdateDialog = dev.techda.sahatisukari.data.UpdateStartupPolicy.shouldShowPrompt(
                        available = result.available,
                        dismissedVersionCode = dismissedUpdateVersion,
                        availableVersionCode = result.availableVersionCode,
                    )
                }
            }.onFailure { error ->
                android.util.Log.w("AppUpdate", "Foreground manifest check failed (${error.javaClass.simpleName})")
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
                dismissedUpdateVersion = update.availableVersionCode
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
                    dismissedUpdateVersion = update.availableVersionCode
                }) { Text("لاحقاً") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientApp(user: AuthUser, onLogout: () -> Unit, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    var navigation by rememberSaveable(stateSaver = dashboardQuickAddFlowSaver) { mutableStateOf(DashboardQuickAddFlow()) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val quickAdd = navigation.request
    BackHandler(enabled = quickAdd != null) { navigation = navigation.cancel() }
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
                    IconButton(onClick = { settings = true; emergency = false; if (quickAdd != null) navigation = navigation.cancel() }) { Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { emergency = true; if (quickAdd != null) navigation = navigation.cancel() }) { Icon(Icons.Outlined.Sos, contentDescription = "مساعدة عاجلة", tint = MaterialTheme.colorScheme.error) }
                    IconButton(onClick = { if (quickAdd != null) navigation = navigation.cancel(); onLogout() }) { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "خروج", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SahatiBottomNavigation(
                selectedIndex = if (emergency || settings) -1 else navigation.section,
                onSelect = { navigation = navigation.selectSection(it); emergency = false; settings = false },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (emergency) EmergencyHelp(Modifier.fillMaxSize().padding(padding))
        else if (settings) SettingsScreen(onBack = { settings = false }, onSignedOut = { settings = false; onLogout() }, modifier = Modifier.fillMaxSize().padding(padding), viewModel = settingsViewModel)
        else if (navigation.section == 0) {
            navigation.notice?.let { notice -> LaunchedEffect(notice) { snackbarHostState.showSnackbar(notice); navigation = navigation.copy(notice = null) } }
            DashboardScreen(
                user,
                Modifier.fillMaxSize().padding(padding),
                onGlucose = { navigation = navigation.selectSection(2) },
                onJournal = { navigation = navigation.selectSection(4).copy(journalEndpoint = "activities") },
                onReports = { navigation = navigation.selectSection(5) },
                onProfile = { navigation = navigation.selectSection(1) },
                onQuickAdd = { action -> navigation = navigation.open(action) },
                refreshRequest = navigation.dashboardRefreshCount,
            )
        } else PatientSectionScreen(
            navigation.section,
            Modifier.fillMaxSize().padding(padding),
            initialJournalEndpoint = navigation.journalEndpoint,
            quickAddRequest = quickAdd,
            onQuickAddCancel = { navigation = navigation.cancel() },
            onQuickAddSaveResult = { succeeded ->
                navigation = navigation.completeSave(succeeded)
            },
        )
    }
}

private val dashboardQuickAddFlowSaver = mapSaver(
    save = { flow ->
        mapOf(
            "section" to flow.section,
            "journalEndpoint" to flow.journalEndpoint,
            "action" to flow.request?.action?.name,
            "requestId" to flow.request?.requestId,
            "refreshCount" to flow.dashboardRefreshCount,
            "notice" to flow.notice,
            "nextRequestId" to flow.nextRequestId,
        )
    },
    restore = { saved ->
        val action = (saved["action"] as? String)?.let { value -> DashboardQuickAdd.entries.firstOrNull { it.name == value } }
        val requestId = saved["requestId"] as? Int
        DashboardQuickAddFlow(
            section = saved["section"] as? Int ?: 0,
            journalEndpoint = saved["journalEndpoint"] as? String ?: "meals",
            request = action?.let { selected -> DashboardQuickAddRequest(selected, requestId ?: 1) },
            dashboardRefreshCount = saved["refreshCount"] as? Int ?: 0,
            notice = saved["notice"] as? String,
            nextRequestId = saved["nextRequestId"] as? Int ?: requestId ?: 0,
        )
    },
)

@Composable
private fun DashboardScreen(user: AuthUser, modifier: Modifier = Modifier, onGlucose: () -> Unit, onJournal: () -> Unit, onReports: () -> Unit, onProfile: () -> Unit, onQuickAdd: (DashboardQuickAdd) -> Unit, refreshRequest: Int, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(refreshRequest) { if (refreshRequest > 0) viewModel.refresh() }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = SahatiUiTokens.screenHorizontal, vertical = SahatiUiTokens.screenVertical), verticalArrangement = Arrangement.spacedBy(SahatiUiTokens.sectionSpacing)) {
        Text("لوحة المتابعة الصحية", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        val profile = (state as? DashboardState.Ready)?.data?.obj("profile")
        val patientName = profile?.string("fullName") ?: user.email
        val zone = profile?.string("timezone") ?: "Asia/Baghdad"
        Text("مرحبًا $patientName — اليوم ${todayDate(zone, profile?.string("numeralSystem"))}", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
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
            is DashboardState.Ready -> DashboardContent(
                current.data,
                timeZoneId = zone,
                numeralSystem = profile?.string("numeralSystem"),
                glucoseUnit = profile?.string("preferredGlucoseUnit") ?: "MG_DL",
                onProfile = onProfile,
                onGlucose = onGlucose,
                onJournal = onJournal,
                onQuickAdd = onQuickAdd,
            )
        }
        Text("هذه اللوحة تعرض السجلات التي أضفتها فقط. لا تقدم تشخيصًا أو توصية علاجية.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    }
}

@Composable
private fun DashboardContent(
    response: JsonObject,
    timeZoneId: String,
    numeralSystem: String?,
    glucoseUnit: String,
    onProfile: () -> Unit,
    onGlucose: () -> Unit,
    onJournal: () -> Unit,
    onQuickAdd: (DashboardQuickAdd) -> Unit,
) {
    val dash = response.obj("dashboard") ?: return
    val completion = dash.obj("completion")
    val percent = completion?.int("percent")
    if (percent != null && percent < 100) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF8F8)), shape = RoundedCornerShape(SahatiUiTokens.cardRadius)) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                Text("أكمل ملفك الصحي لتحصل على متابعة أدق", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
                Text("نسبة الاكتمال الحالية ${MedicalPresentation.localizedDigits(percent.toString(), numeralSystem)}%", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onProfile) { Text("إكمال الملف الصحي") }
            }
        }
    }
    val latest = DashboardPresentation.latestGlucose(dash.obj("latestGlucose"), glucoseUnit, timeZoneId, numeralSystem)
    DashboardLatestGlucoseCard(latest, onGlucose)
    DashboardPresentation.glucoseInformationNote(dash.obj("latestGlucose")?.string("standardizedValueMgdl"), numeralSystem)?.let { note ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(SahatiUiTokens.cardRadius)) {
            Text(note, Modifier.padding(SahatiUiTokens.cardPadding), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
    StatCard("قياسات اليوم", dash.string("todayReadings")?.let { MedicalPresentation.formatDecimal(it, 0, numeralSystem) } ?: "—", if (dash.int("todayReadings") == 0) "لم تسجل أي قراءة اليوم" else "قراءات مسجّلة اليوم", Icons.Outlined.Assessment)
    DashboardLatestAdministrationCard(DashboardPresentation.latestAdministration(dash.obj("lastAdministration"), timeZoneId, numeralSystem))
    val activityMinutes = dash.string("activityMinutes") ?: "0"
    StatCard("نشاط اليوم", if ((activityMinutes.toIntOrNull() ?: 0) == 0) "—" else MedicalPresentation.formatActivityDuration(activityMinutes, numeralSystem), "${MedicalPresentation.localizedDigits((dash.array("todayActivities")?.size ?: 0).toString(), numeralSystem)} نشاط مسجّل", Icons.Outlined.CalendarMonth)
    DashboardQuickAddCard(onQuickAdd)
    val trendPoints = DashboardPresentation.trendPoints(dash.array("trendData"))
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("اتجاه قراءات السكر (آخر 14 يومًا)", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            DashboardTrendChart(trendPoints, glucoseUnit, timeZoneId, numeralSystem)
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Text("ملخص اليوم", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                DashboardPresentation.todaySummary(dash, numeralSystem).forEach { SummaryRow(it.label, it.value) }
            }
        }
    }
    val eventDeltas = DashboardPresentation.eventDeltas(dash, timeZoneId, numeralSystem)
    if (eventDeltas.isNotEmpty()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("فروق متوسط القراءات مع الأحداث المسجّلة (آخر 14 يومًا)", fontWeight = FontWeight.Bold)
                Text("يُحسب الفرق بين متوسط القراءات قبل الحدث بثلاث ساعات وبعده بثلاث ساعات اعتمادًا على القراءات المسجّلة فقط. هذا وصف زمني ولا يعني أن الحدث سبّب التغيّر.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                eventDeltas.forEach { event ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(event.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        MedicalValueText(event.at, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        val delta = event.delta
                        if (delta == null) {
                            Text("لا توجد قراءات كافية قبل الحدث وبعده لعرض الفرق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val decimals = if (glucoseUnit == "MMOL_L") 1 else 0
                            val before = MedicalPresentation.formatDecimal(event.before?.toString(), decimals, numeralSystem)
                            val after = MedicalPresentation.formatDecimal(event.after?.toString(), decimals, numeralSystem)
                            val difference = MedicalPresentation.formatDecimal(kotlin.math.abs(delta).toString(), decimals, numeralSystem)
                            val unitLabel = DashboardPresentation.glucoseUnitLabel(glucoseUnit)
                            MedicalValueText("متوسط قبل: $before $unitLabel — بعد: $after $unitLabel — الفرق: ${if (delta > 0) "+" else if (delta < 0) "−" else ""}$difference $unitLabel", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    DashboardRecentGlucoseCard(DashboardPresentation.recentGlucose(dash.array("recentGlucose"), glucoseUnit, timeZoneId, numeralSystem), onGlucose)
    DashboardTodayActivitiesCard(DashboardPresentation.todayActivities(dash.array("todayActivities"), timeZoneId, numeralSystem), onJournal)
}

@Composable
private fun DashboardQuickAddCard(onQuickAdd: (DashboardQuickAdd) -> Unit) {
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إضافة سريعة", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dashboardQuickAddRows[0].forEach { action ->
                        val (label, icon) = when (action) {
                            DashboardQuickAdd.Glucose -> "إضافة قياس سكر" to Icons.Outlined.Bloodtype
                            DashboardQuickAdd.Medication -> "تسجيل دواء" to Icons.Outlined.Medication
                            else -> error("Unexpected first-row Quick Add action")
                        }
                        QuickAddButton(label, icon, Modifier.weight(1f), onClick = { onQuickAdd(action) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dashboardQuickAddRows[1].forEach { action ->
                        val (label, icon) = when (action) {
                            DashboardQuickAdd.Meal -> "تسجيل وجبة" to Icons.Outlined.Restaurant
                            DashboardQuickAdd.Activity -> "تسجيل نشاط بدني" to Icons.AutoMirrored.Outlined.DirectionsRun
                            else -> error("Unexpected second-row Quick Add action")
                        }
                        QuickAddButton(label, icon, Modifier.weight(1f), onClick = { onQuickAdd(action) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DashboardLatestGlucoseCard(record: DashboardGlucoseCard?, onGlucose: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("آخر قراءة سكر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MedicalValueText(record?.value ?: "—", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            if (record == null) {
                Text("لم تسجل أي قراءة بعد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                record.context?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                record.dateTime?.let { MedicalValueText(it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }
            }
            TextButton(onClick = onGlucose) { Text("كل القياسات") }
        }
    }
}

@Composable
private fun DashboardLatestAdministrationCard(record: DashboardAdministrationCard?) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("آخر جرعة دواء مسجّلة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record == null) {
                Text("لا توجد جرعات مسجّلة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(record.status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                record.medicationName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                record.actualDose?.let { MedicalValueText("الجرعة المسجّلة: $it", style = MaterialTheme.typography.bodySmall) }
                record.dateTime?.let { MedicalValueText(it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }
            }
        }
    }
}

@Composable
private fun DashboardRecentGlucoseCard(items: List<DashboardGlucoseCard>, onGlucose: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("أحدث قراءات السكر", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) Text("لم تسجل أي قراءة للسكر بعد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else items.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MedicalValueText(item.value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(item.context ?: "سياق غير مسجّل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.dateTime?.let { MedicalValueText(it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }
                }
            }
            TextButton(onClick = onGlucose) { Text("إدارة القياسات") }
        }
    }
}

@Composable
private fun DashboardTodayActivitiesCard(items: List<DashboardActivityItem>, onJournal: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("نشاطات اليوم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) Text("لا يوجد نشاط بدني مسجّل اليوم.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else items.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.type, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(item.duration, item.intensity).joinToString(" — "), style = MaterialTheme.typography.bodySmall)
                    item.time?.let { MedicalValueText(it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }
                }
            }
            TextButton(onClick = onJournal) { Text("يومياتي") }
        }
    }
}

@Composable
internal fun StatCard(label: String, value: String, detail: String, icon: ImageVector) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SahatiUiTokens.cardRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(SahatiUiTokens.cardPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4A90D9), modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MedicalValueText(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                if (detail.isNotBlank()) MedicalValueText(detail, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
    }
}

@Composable
internal fun SummaryRow(label: String, value: String) {
    MedicalLabelValueRow(
        label = label,
        value = value,
        labelStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        valueStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
    )
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

private fun todayDate(zone: String, numeralSystem: String?): String = MedicalPresentation.formatToday(zone, numeralSystem)
private fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)
private fun JsonObject.array(key: String): JsonArray? = (this[key] as? JsonArray)
private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()
