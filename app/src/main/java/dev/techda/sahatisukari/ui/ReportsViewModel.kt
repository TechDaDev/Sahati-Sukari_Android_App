package dev.techda.sahatisukari.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.techda.sahatisukari.data.PatientApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

data class ReportsState(val summary: JsonObject? = null, val file: File? = null, val busy: Boolean = false, val error: String? = null)

@HiltViewModel
class ReportsViewModel @Inject constructor(private val api: PatientApi, @ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    fun load(from: String, to: String) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, error = null)
        runCatching {
            val response = api.reportSummary(mapOf("from" to from, "to" to to))
            if (!response.isSuccessful) error("تعذر تحميل ملخص التقرير (${response.code()}).")
            response.body()?.data ?: error("لم يرد ملخص التقرير.")
        }.onSuccess { _state.value = _state.value.copy(summary = it, busy = false) }
            .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر تحميل التقرير.", busy = false) }
    }

    fun download(from: String, to: String) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, error = null)
        runCatching {
            val response = api.reportPdf(mapOf("from" to from, "to" to to))
            if (!response.isSuccessful) error("تعذر تنزيل التقرير (${response.code()}).")
            val body = response.body() ?: error("ملف التقرير فارغ.")
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "reports").apply { mkdirs() }
                val tmp = File(dir, "health-report.pdf.tmp")
                val file = File(dir, "health-report.pdf")
                body.byteStream().use { input -> tmp.outputStream().use(input::copyTo) }
                if (!tmp.renameTo(file)) { tmp.copyTo(file, overwrite = true); tmp.delete() }
                file
            }
        }.onSuccess { _state.value = _state.value.copy(file = it, busy = false) }
            .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر تنزيل التقرير.", busy = false) }
    }
}
