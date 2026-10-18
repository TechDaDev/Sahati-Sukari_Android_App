package dev.techda.sahatisukari.ui

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

@Composable
fun ReportsScreen(modifier: Modifier = Modifier, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var from by remember { mutableStateOf(LocalDate.now().minusDays(30).toString()) }
    var to by remember { mutableStateOf(LocalDate.now().toString()) }
    var fetched by remember { mutableStateOf(false) }
    val rangeValid = runCatching { !LocalDate.parse(from).isAfter(LocalDate.parse(to)) && !LocalDate.parse(from).isBefore(LocalDate.parse(to).minusDays(366)) }.getOrDefault(false)
    LaunchedEffect(state.file) {
        val file = state.file ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)) }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("التقارير", Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        OutlinedTextField(from, { from = it }, Modifier.fillMaxWidth(), label = { Text("من (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(to, { to = it }, Modifier.fillMaxWidth(), label = { Text("إلى (YYYY-MM-DD)") }, singleLine = true)
        Button(onClick = { fetched = true; viewModel.load(from, to) }, enabled = rangeValid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("عرض ملخص التقرير") }
        Button(onClick = { viewModel.download(from, to) }, enabled = rangeValid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("تنزيل ملف PDF") }
        OutlinedButton(onClick = {
            val file = state.file ?: return@OutlinedButton
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, "مشاركة التقرير الصحي"))
        }, enabled = state.file != null, modifier = Modifier.fillMaxWidth()) { Text("مشاركة التقرير") }
        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        state.error?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End) }
        if (fetched && state.summary != null) JsonSummary(state.summary!!)
        Text("يُحفظ الملف داخل مساحة التطبيق الخاصة، ولا يُشارك إلا عند اختيارك المشاركة.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

@Composable
private fun JsonSummary(summary: JsonObject) {
    val entries = flatten(summary)
    if (entries.isEmpty()) {
        Text("لا توجد بيانات ضمن هذه الفترة.", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        return
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ملخص الفترة", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            entries.take(30).forEach { (label, value) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(value, fontWeight = FontWeight.SemiBold); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

private fun flatten(value: JsonObject, prefix: String = ""): List<Pair<String, String>> = value.flatMap { (key, item) ->
    val label = if (prefix.isEmpty()) key else "$prefix / $key"
    when (item) {
        is JsonObject -> flatten(item, label)
        is JsonArray -> listOf(label to "${item.size}")
        else -> listOf(label to (runCatching { item.jsonPrimitive.content }.getOrDefault("")))
    }
}.filter { it.second.isNotBlank() }
