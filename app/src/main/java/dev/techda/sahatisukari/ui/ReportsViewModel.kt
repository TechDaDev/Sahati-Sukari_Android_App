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

data class ReportsState(
    val summary: JsonObject? = null,
    val file: File? = null,
    val fileFrom: String? = null,
    val fileTo: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(private val api: PatientApi, @param:ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        deleteCachedReports()
    }

    fun load(from: String, to: String) = viewModelScope.launch {
        _state.value = _state.value.copy(summary = null, busy = true, error = null)
        runCatching {
            val response = api.reportSummary(mapOf("from" to from, "to" to to))
            if (!response.isSuccessful) error("تعذر تحميل ملخص التقرير (${response.code()}).")
            response.body()?.data ?: error("لم يرد ملخص التقرير.")
        }.onSuccess { _state.value = _state.value.copy(summary = it, busy = false) }
            .onFailure { _state.value = _state.value.copy(summary = null, error = it.message ?: "تعذر تحميل التقرير.", busy = false) }
    }

    fun download(from: String, to: String) = viewModelScope.launch {
        _state.value = _state.value.copy(file = null, fileFrom = null, fileTo = null, busy = true, error = null)
        runCatching {
            withContext(Dispatchers.IO) { deleteCachedReports() }
            val response = api.reportPdf(mapOf("from" to from, "to" to to))
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                error(pdfHttpError(response.code()))
            }
            val body = response.body() ?: error("ملف التقرير فارغ.")
            withContext(Dispatchers.IO) {
                ReportPdfStorage.save(body.byteStream(), body.contentLength(), File(context.cacheDir, "reports"))
            }
        }.onSuccess {
            _state.value = _state.value.copy(file = it, fileFrom = from, fileTo = to, busy = false)
        }.onFailure {
            withContext(Dispatchers.IO) { deleteCachedReports() }
            _state.value = _state.value.copy(
                file = null,
                fileFrom = null,
                fileTo = null,
                error = it.message ?: "تعذر تنزيل التقرير. تحقق من اتصالك ثم أعد المحاولة.",
                busy = false,
            )
        }
    }

    fun invalidatePdf() {
        _state.value = _state.value.copy(file = null, fileFrom = null, fileTo = null)
        viewModelScope.launch(Dispatchers.IO) { deleteCachedReports() }
    }

    fun showError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    private fun deleteCachedReports() {
        File(context.cacheDir, "reports").listFiles()?.forEach(File::delete)
    }

    private fun pdfHttpError(status: Int): String = when (status) {
        401 -> "انتهت جلسة الدخول. سجّل الدخول مجددًا ثم أعد المحاولة."
        403 -> "لا تملك صلاحية تنزيل هذا التقرير."
        503 -> "خدمة إنشاء التقرير غير متاحة حاليًا. حاول لاحقًا."
        else -> "تعذر تنزيل التقرير (HTTP $status)."
    }
}
