package dev.techda.sahatisukari.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.techda.sahatisukari.data.PatientApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

sealed interface DashboardState {
    data object Loading : DashboardState
    data class Ready(val data: JsonObject) : DashboardState
    data class Failed(val message: String) : DashboardState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(private val api: PatientApi) : ViewModel() {
    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() {
        _state.value = DashboardState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                val response = api.dashboard()
                if (!response.isSuccessful) error(if (response.code() in 500..599) "الخدمة غير متاحة مؤقتًا." else "انتهت الجلسة. سجّل الدخول مجددًا.")
                response.body()?.data ?: error("تعذر قراءة بيانات اللوحة.")
            }.fold({ DashboardState.Ready(it) }, { DashboardState.Failed(it.message ?: "تعذر تحميل البيانات.") })
        }
    }
}
