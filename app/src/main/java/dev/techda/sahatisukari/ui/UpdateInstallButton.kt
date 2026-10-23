package dev.techda.sahatisukari.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.techda.sahatisukari.data.UpdateApkInstaller
import dev.techda.sahatisukari.data.UpdateCheck
import dev.techda.sahatisukari.data.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun UpdateInstallButton(update: UpdateCheck) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    var busy by remember(update.availableVersionCode) { mutableStateOf(false) }
    var progress by remember(update.availableVersionCode) { mutableIntStateOf(-1) }
    var status by remember(update.availableVersionCode) { mutableStateOf<String?>(null) }
    var downloadJob by remember(update.availableVersionCode) { mutableStateOf<Job?>(null) }
    var showPermissionDialog by rememberSaveable(update.availableVersionCode) { mutableStateOf(false) }
    var waitingForPermission by rememberSaveable(update.availableVersionCode) { mutableStateOf(false) }

    fun launchVerifiedApk(apk: java.io.File) {
        val foregroundActivity = activity
        if (foregroundActivity == null) {
            status = "تعذّر فتح مثبت Android من الشاشة الحالية. أعد المحاولة والتطبيق مفتوح."
            return
        }
        try {
            UpdateApkInstaller.launchInstaller(foregroundActivity, apk, update.installedVersionCode)
            status = "تم التحقق من التحديث. أكمل التثبيت في نافذة Android."
        } catch (error: Exception) {
            android.util.Log.w("AppUpdate", "Installer launch failed (${error.javaClass.simpleName})")
            status = error.message ?: "رفض Android فتح مثبت التطبيق. أعد المحاولة من فضلك."
        }
    }

    val permissionSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (waitingForPermission) {
            waitingForPermission = false
            if (UpdateApkInstaller.canRequestPackageInstalls(context)) {
                busy = true
                downloadJob = scope.launch {
                    try {
                        val cachedApk = UpdateRepository.shared.cachedApkForInstall(context, update.manifest)
                            ?: error("تعذّر العثور على ملف التحديث الذي تم التحقق منه. اضغط تحديث الآن لإعادة تنزيله.")
                        launchVerifiedApk(cachedApk)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        android.util.Log.w("AppUpdate", "Resume after install permission failed (${error.javaClass.simpleName})")
                        status = error.message ?: "تعذّر متابعة تثبيت التحديث."
                    } finally {
                        busy = false
                    }
                }
            } else {
                status = "لم يتم السماح بالتثبيت. يمكنك المتابعة أو فتح الإعدادات لاحقاً من جديد."
            }
        }
    }

    Column {
        Button(
            onClick = {
                if (busy) return@Button
                val foregroundActivity = activity
                if (foregroundActivity == null) {
                    status = "تعذّر فتح مثبت Android من الشاشة الحالية. أعد المحاولة والتطبيق مفتوح."
                    return@Button
                }
                busy = true
                status = null
                downloadJob = scope.launch {
                    var stage = "download_and_verification"
                    try {
                        val apk = UpdateRepository.shared.apkForInstall(context, update.manifest) { downloaded, total ->
                            progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else -1
                        }
                        stage = "permission_check"
                        if (!UpdateApkInstaller.canRequestPackageInstalls(context)) {
                            showPermissionDialog = true
                        } else {
                            stage = "installer_launch"
                            launchVerifiedApk(apk)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        android.util.Log.w("AppUpdate", "Update action failed at $stage (${error.javaClass.simpleName})")
                        status = error.message ?: "تعذّر تنزيل التحديث. يرجى المحاولة لاحقاً."
                    } finally {
                        busy = false
                        progress = -1
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "جارٍ تنزيل التحديث" else "تحديث الآن")
        }
        if (busy) {
            if (progress < 0) androidx.compose.material3.CircularProgressIndicator()
            else LinearProgressIndicator({ progress / 100f }, Modifier.fillMaxWidth())
            Text(if (progress >= 0) "جارٍ تنزيل التحديث: $progress٪" else "جارٍ تنزيل التحديث…", Modifier.fillMaxWidth())
            TextButton(onClick = { downloadJob?.cancel(); status = "تم إلغاء التنزيل." }, modifier = Modifier.fillMaxWidth()) { Text("إلغاء التنزيل") }
        }
        status?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("السماح بتثبيت التحديث") },
            text = { Text("يتطلب Android موافقتك للسماح لتطبيق صحتي وسكري بتثبيت التحديثات. سنفتح إعدادات هذا التطبيق، وبعد عودتك سنواصل تثبيت الملف الذي تم تنزيله والتحقق منه.") },
            confirmButton = {
                TextButton(onClick = {
                    val foregroundActivity = activity
                    if (foregroundActivity == null) {
                        showPermissionDialog = false
                        status = "تعذّر فتح إعدادات تثبيت التحديث من الشاشة الحالية."
                    } else {
                        showPermissionDialog = false
                        waitingForPermission = true
                        permissionSettingsLauncher.launch(UpdateApkInstaller.unknownSourcesSettingsIntent(context))
                    }
                }) { Text("فتح الإعدادات") }
            },
            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("لاحقاً") } },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
