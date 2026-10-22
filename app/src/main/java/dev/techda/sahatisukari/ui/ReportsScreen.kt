package dev.techda.sahatisukari.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate

@Composable
fun ReportsScreen(modifier: Modifier = Modifier, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var from by remember { mutableStateOf(LocalDate.now().minusDays(30).toString()) }
    var to by remember { mutableStateOf(LocalDate.now().toString()) }
    var fetched by remember { mutableStateOf(false) }
    val rangeValid = runCatching { !LocalDate.parse(from).isAfter(LocalDate.parse(to)) && !LocalDate.parse(from).isBefore(LocalDate.parse(to).minusDays(366)) }.getOrDefault(false)
    val currentPdf = state.file?.takeIf { state.fileFrom == from && state.fileTo == to }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SahatiPageTitle("التقارير")
        OutlinedTextField(from, { from = it; viewModel.invalidatePdf() }, Modifier.fillMaxWidth(), label = { Text("من (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(to, { to = it; viewModel.invalidatePdf() }, Modifier.fillMaxWidth(), label = { Text("إلى (YYYY-MM-DD)") }, singleLine = true)
        Button(onClick = { fetched = true; viewModel.load(from, to) }, enabled = rangeValid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("عرض ملخص التقرير") }
        Button(onClick = { viewModel.download(from, to) }, enabled = rangeValid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تنزيل ملف PDF") }
        OutlinedButton(onClick = {
            val file = currentPdf ?: return@OutlinedButton
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            try {
                context.startActivity(view)
            } catch (_: ActivityNotFoundException) {
                viewModel.showError("لا يوجد تطبيق لعرض ملفات PDF على هذا الجهاز.")
            }
        }, enabled = currentPdf != null && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("عرض ملف PDF") }
        OutlinedButton(onClick = {
            val file = currentPdf ?: return@OutlinedButton
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                context.startActivity(Intent.createChooser(send, "مشاركة التقرير الصحي"))
            } catch (_: ActivityNotFoundException) {
                viewModel.showError("تعذر فتح قائمة مشاركة التقرير على هذا الجهاز.")
            }
        }, enabled = currentPdf != null && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("مشاركة التقرير") }
        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
        if (fetched && state.summary != null) JsonSummary(state.summary!!)
        Text("يُحفظ الملف داخل مساحة التطبيق الخاصة، ولا يُشارك إلا عند اختيارك المشاركة.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    }
}

@Composable
private fun JsonSummary(summary: kotlinx.serialization.json.JsonObject) {
    val presentation = presentReportSummary(summary)
    if (presentation.isEmpty) {
        Text("لا توجد بيانات ضمن هذه الفترة.", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        return
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ملخص الفترة", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
            presentation.rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(value, fontWeight = FontWeight.SemiBold)
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
