package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
    val scope = rememberCoroutineScope()
    var busy by remember(update.availableVersionCode) { mutableStateOf(false) }
    var progress by remember(update.availableVersionCode) { mutableIntStateOf(-1) }
    var status by remember(update.availableVersionCode) { mutableStateOf<String?>(null) }
    var downloadJob by remember(update.availableVersionCode) { mutableStateOf<Job?>(null) }

    Column {
        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                status = null
                downloadJob = scope.launch {
                    try {
                        val apk = UpdateRepository.shared.apkForInstall(context, update.availableVersionCode) { downloaded, total ->
                            progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else -1
                        }
                        if (UpdateApkInstaller.requestInstall(context, apk, update.installedVersionCode.toInt())) {
                            status = "تم التحقق من الإصدار والتوقيع. أكمل التثبيت في نافذة Android."
                        } else {
                            status = "اسمح بالتثبيت من هذا المصدر في إعدادات Android، ثم اضغط تحديث الآن للمتابعة دون إعادة التنزيل."
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
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
}
