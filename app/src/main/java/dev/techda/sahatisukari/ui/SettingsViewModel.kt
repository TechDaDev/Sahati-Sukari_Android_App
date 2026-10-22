package dev.techda.sahatisukari.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.techda.sahatisukari.data.AuthApi
import dev.techda.sahatisukari.data.ChangePasswordRequest
import dev.techda.sahatisukari.data.PatientApi
import dev.techda.sahatisukari.data.PatientRequestCodec
import dev.techda.sahatisukari.data.TokenStore
import dev.techda.sahatisukari.data.UpdateCheck
import dev.techda.sahatisukari.data.UpdateRepository
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

data class SettingsState(val sessions: JsonObject? = null, val email: String = "", val phone: String = "", val glucoseUnit: String = "MG_DL", val numerals: String = "LATIN", val busy: Boolean = false, val error: String? = null, val notice: String? = null, val signedOut: Boolean = false, val updateBusy: Boolean = false, val updateError: String? = null, val updateCheckedAt: String? = null, val updateCheck: UpdateCheck? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @Named("session") private val authApi: AuthApi,
    private val patientApi: PatientApi,
    private val tokenStore: TokenStore,
    private val json: Json,
    private val updateRepository: UpdateRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun checkForUpdates(installedVersionCode: Int) {
        if (_state.value.updateBusy) return
        _state.value = _state.value.copy(updateBusy = true, updateError = null)
        viewModelScope.launch {
            runCatching { updateRepository.check(installedVersionCode) }
                .onSuccess { _state.value = _state.value.copy(updateBusy = false, updateCheck = it, updateCheckedAt = java.time.Instant.ofEpochMilli(it.checkedAtMillis).toString()) }
                .onFailure { _state.value = _state.value.copy(updateBusy = false, updateError = "تعذر الاتصال بخدمة التحديث. حاول لاحقًا.") }
        }
    }

    fun loadInitial() = request {
        val profileResponse = patientApi.profile()
        if (!profileResponse.isSuccessful) fail(profileResponse.code(), profileResponse.errorBody()?.string())
        val profile = profileResponse.body()?.data?.get("profile")?.let { runCatching { it.jsonObject }.getOrNull() } ?: profileResponse.body()?.data
        val unit = profile?.get("preferredGlucoseUnit")?.jsonPrimitive?.contentOrNull ?: "MG_DL"
        val numerals = profile?.get("numeralSystem")?.jsonPrimitive?.contentOrNull ?: "LATIN"
        val sessionsResponse = authApi.sessions()
        if (!sessionsResponse.isSuccessful) fail(sessionsResponse.code(), sessionsResponse.errorBody()?.string())
        _state.value = _state.value.copy(email = profile?.get("email")?.jsonPrimitive?.contentOrNull.orEmpty(), phone = profile?.get("phone")?.jsonPrimitive?.contentOrNull.orEmpty(), glucoseUnit = unit, numerals = numerals, sessions = sessionsResponse.body()?.data, busy = false)
    }

    fun loadSessions() = request {
        val response = authApi.sessions()
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        _state.value = _state.value.copy(sessions = response.body()?.data, busy = false)
    }

    fun loadPreferences() = request {
        val response = patientApi.profile()
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        val profile = response.body()?.data?.get("profile")?.let { runCatching { it.jsonObject }.getOrNull() } ?: response.body()?.data
        val unit = profile?.get("preferredGlucoseUnit")?.jsonPrimitive?.contentOrNull ?: "MG_DL"
        val numerals = profile?.get("numeralSystem")?.jsonPrimitive?.contentOrNull ?: "LATIN"
        _state.value = _state.value.copy(glucoseUnit = unit, numerals = numerals, busy = false)
    }

    fun savePreferences(unit: String, numerals: String) = request {
        val body = buildJsonObject { put("preferredGlucoseUnit", unit); put("numeralSystem", numerals) }
        val response = patientApi.updateContact(PatientRequestCodec.encode(json, "settings/contact", body))
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        _state.value = _state.value.copy(glucoseUnit = unit, numerals = numerals, busy = false, notice = "تم حفظ التفضيلات.")
    }

    fun saveContact(email: String, phone: String) = request {
        val body = buildJsonObject {
            if (email.isNotBlank()) put("email", email.trim())
            if (phone.isNotBlank()) put("phone", phone.trim())
        }
        val response = patientApi.updateContact(PatientRequestCodec.encode(json, "settings/contact", body))
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        _state.value = _state.value.copy(busy = false, email = email.ifBlank { _state.value.email }, phone = phone.ifBlank { _state.value.phone }, notice = "تم تحديث معلومات الحساب.")
    }

    fun changePassword(current: String, next: String, confirm: String) = request {
        val response = authApi.changePassword(ChangePasswordRequest(current, next, confirm))
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        tokenStore.clear()
        _state.value = _state.value.copy(busy = false, signedOut = true, notice = "تغيرت كلمة المرور. سجّل الدخول مجددًا.")
    }

    fun logoutAll() = request {
        val response = authApi.logoutAll()
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        tokenStore.clear()
        _state.value = _state.value.copy(busy = false, signedOut = true, notice = "تم تسجيل الخروج من جميع الأجهزة.")
    }

    fun revokeSession(id: String) = request {
        val response = authApi.deleteSession(id)
        if (!response.isSuccessful) fail(response.code(), response.errorBody()?.string())
        _state.value = _state.value.copy(busy = false, notice = "تم إنهاء الجلسة.")
        loadSessions()
    }

    private fun request(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null, notice = null)
        viewModelScope.launch { runCatching { action() }.onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "تعذر تنفيذ الطلب.") } }
    }

    private fun fail(status: Int, body: String?): Nothing {
        val message = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body.orEmpty()).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
        throw IllegalStateException(message ?: when (status) { 401 -> "انتهت الجلسة. سجّل الدخول مجددًا."; 403 -> "لا تملك صلاحية هذا الطلب."; 429 -> "طلبات كثيرة. حاول لاحقًا."; in 500..599 -> "الخدمة غير متاحة مؤقتًا."; else -> "تعذر تنفيذ الطلب. تحقق من البيانات." })
    }
}
