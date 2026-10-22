package dev.techda.sahatisukari.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.techda.sahatisukari.data.PatientApi
import dev.techda.sahatisukari.data.PatientRequestCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class PatientSectionState(
    val endpoint: String = "",
    val data: JsonObject? = null,
    val relatedEndpoint: String = "",
    val relatedData: JsonObject? = null,
    val profileWeights: JsonObject? = null,
    val profileLabs: JsonObject? = null,
    val profileHistoryLoading: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

@HiltViewModel
class PatientSectionViewModel @Inject constructor(private val api: PatientApi, private val json: Json) : ViewModel() {
    private val _state = MutableStateFlow(PatientSectionState())
    val state: StateFlow<PatientSectionState> = _state.asStateFlow()

    fun load(endpoint: String) {
        if (_state.value.endpoint == endpoint && (_state.value.loading || _state.value.data != null)) return
        val notice = _state.value.notice
        _state.value = PatientSectionState(endpoint = endpoint, loading = true, notice = notice)
        viewModelScope.launch {
            runCatching {
                val response = if (endpoint == "profile") api.profile() else api.list(endpoint, mapOf("limit" to "50", "sort" to "desc"))
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                response.body()?.data ?: error("تعذر قراءة البيانات.")
            }.onSuccess { _state.value = _state.value.copy(endpoint = endpoint, data = it, loading = false, error = null, notice = notice) }
                .onFailure { _state.value = _state.value.copy(endpoint = endpoint, loading = false, error = it.message ?: "تعذر تحميل البيانات.", notice = notice) }
        }
    }

    fun reload() {
        val endpoint = _state.value.endpoint
        if (endpoint.isNotBlank()) {
            val notice = _state.value.notice
            _state.value = PatientSectionState(notice = notice)
            load(endpoint)
            if (endpoint == "medications") loadRelated("administrations")
            if (endpoint == "profile") loadProfileHistory(force = true)
        }
    }

    fun loadRelated(endpoint: String) {
        if (_state.value.relatedEndpoint == endpoint && _state.value.relatedData != null) return
        viewModelScope.launch {
            runCatching {
                val response = api.list(endpoint, mapOf("limit" to "50", "sort" to "desc"))
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                response.body()?.data ?: error("تعذر قراءة البيانات.")
            }.onSuccess { _state.value = _state.value.copy(relatedEndpoint = endpoint, relatedData = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر تحميل البيانات.") }
        }
    }

    fun loadProfileHistory(force: Boolean = false) {
        val current = _state.value
        if (current.profileHistoryLoading || (!force && current.profileWeights != null && current.profileLabs != null)) return
        _state.value = current.copy(profileHistoryLoading = true)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    suspend fun fetch(endpoint: String): JsonObject {
                        val response = api.list(endpoint, mapOf("limit" to "50", "sort" to "desc"))
                        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                        return response.body()?.data ?: error("تعذر قراءة البيانات.")
                    }
                    val weights = async { fetch("weight") }
                    val labs = async { fetch("labs") }
                    weights.await() to labs.await()
                }
            }.onSuccess { (weights, labs) ->
                _state.value = _state.value.copy(profileWeights = weights, profileLabs = labs, profileHistoryLoading = false, error = null)
            }.onFailure {
                _state.value = _state.value.copy(profileHistoryLoading = false, error = "تعذر تحميل سجل الوزن والتحاليل.")
            }
        }
    }

    fun loadMoreProfileHistory(endpoint: String) {
        val current = _state.value
        val oldData = if (endpoint == "weight") current.profileWeights else current.profileLabs
        val page = oldData?.get("pagination")?.let { runCatching { it.jsonObject }.getOrNull() } ?: return
        val cursor = page["nextCursor"]?.jsonPrimitive?.contentOrNull ?: return
        if (current.busy || endpoint !in setOf("weight", "labs")) return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching {
                val response = api.list(endpoint, mapOf("limit" to "50", "sort" to "desc", "cursor" to cursor))
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                response.body()?.data ?: error("تعذر تحميل الصفحة التالية.")
            }.onSuccess { next ->
                val oldItems = (oldData?.get("items") as? JsonArray).orEmpty()
                val newItems = (next["items"] as? JsonArray).orEmpty()
                val merged = buildJsonObject {
                    oldData?.forEach { (key, value) -> put(key, value) }
                    put("items", JsonArray(oldItems + newItems))
                    next["pagination"]?.let { put("pagination", it) }
                }
                _state.value = if (endpoint == "weight") current.copy(profileWeights = merged, busy = false) else current.copy(profileLabs = merged, busy = false)
            }.onFailure { _state.value = _state.value.copy(busy = false, error = "تعذر تحميل المزيد من السجلات.") }
        }
    }

    fun loadMore() {
        val current = _state.value
        val endpoint = current.endpoint
        val oldData = current.data ?: return
        val page = oldData["pagination"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return
        val cursor = page["nextCursor"]?.jsonPrimitive?.contentOrNull ?: return
        if (current.busy || endpoint.isBlank()) return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching {
                val response = api.list(endpoint, mapOf("limit" to "50", "sort" to "desc", "cursor" to cursor))
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                response.body()?.data ?: error("تعذر تحميل الصفحة التالية.")
            }.onSuccess { next ->
                val oldItems = (oldData["items"] as? JsonArray).orEmpty()
                val newItems = (next["items"] as? JsonArray).orEmpty()
                val merged = buildJsonObject {
                    oldData.forEach { (key, value) -> put(key, value) }
                    put("items", JsonArray(oldItems + newItems))
                    next["pagination"]?.let { put("pagination", it) }
                }
                _state.value = current.copy(data = merged, busy = false)
            }.onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "تعذر تحميل المزيد.") }
        }
    }

    fun loadRelatedMore() {
        val current = _state.value
        val endpoint = current.relatedEndpoint
        val oldData = current.relatedData ?: return
        val page = oldData["pagination"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return
        val cursor = page["nextCursor"]?.jsonPrimitive?.contentOrNull ?: return
        if (current.busy || endpoint.isBlank()) return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching {
                val response = api.list(endpoint, mapOf("limit" to "50", "sort" to "desc", "cursor" to cursor))
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
                response.body()?.data ?: error("تعذر تحميل الصفحة التالية.")
            }.onSuccess { next ->
                val oldItems = (oldData["items"] as? JsonArray).orEmpty()
                val newItems = (next["items"] as? JsonArray).orEmpty()
                val merged = buildJsonObject {
                    oldData.forEach { (key, value) -> put(key, value) }
                    put("items", JsonArray(oldItems + newItems))
                    next["pagination"]?.let { put("pagination", it) }
                }
                _state.value = current.copy(relatedData = merged, busy = false)
            }.onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "تعذر تحميل المزيد.") }
        }
    }

    fun save(endpoint: String, body: JsonObject, id: String? = null) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching {
                val requestBody = PatientRequestCodec.encode(json, endpoint, body)
                val response = when {
                    endpoint == "profile" -> api.updateProfile(requestBody)
                    endpoint.startsWith("profile:") -> api.updateProfileSection(endpoint.removePrefix("profile:"), requestBody)
                    id != null -> api.update(endpoint, id, requestBody)
                    else -> api.create(endpoint, requestBody)
                }
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
            }.onSuccess {
                _state.value = _state.value.copy(busy = false, notice = "تم الحفظ.")
                reload()
                if (endpoint in setOf("weight", "labs")) loadProfileHistory(force = true)
            }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "تعذر حفظ البيانات.") }
        }
    }

    fun delete(endpoint: String, id: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching {
                val response = api.delete(endpoint, id)
                if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
            }.onSuccess { _state.value = _state.value.copy(busy = false, notice = "تم حذف السجل."); reload() }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "تعذر حذف السجل.") }
        }
    }

    private fun fail(status: Int, body: String?): Nothing {
        val message = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body.orEmpty()).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
        throw IllegalStateException(message ?: when (status) {
            401 -> "انتهت الجلسة. سجّل الدخول مجددًا."
            403 -> "لا تملك صلاحية تنفيذ هذا الطلب."
            404 -> "السجل غير موجود."
            409 -> "تعذر إكمال الطلب بسبب تعارض. حدّث البيانات وحاول مرة أخرى."
            429 -> "طلبات كثيرة. حاول لاحقًا."
            in 500..599 -> "الخدمة غير متاحة مؤقتًا."
            else -> "تعذر إكمال الطلب. راجع البيانات وحاول مجددًا."
        })
    }
}
