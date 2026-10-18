package dev.techda.sahatisukari.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.techda.sahatisukari.data.AuthRepository
import dev.techda.sahatisukari.data.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

data class LoginUiState(val busy: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = runCatching { repository.restoreSession() }.getOrNull()?.let(AuthState::SignedIn) ?: AuthState.SignedOut
        }
    }

    fun login(email: String, password: String) {
        if (_login.value.busy) return
        _login.value = LoginUiState(busy = true)
        viewModelScope.launch {
            runCatching { repository.login(email, password) }
                .onSuccess { _state.value = AuthState.SignedIn(it); _login.value = LoginUiState() }
                .onFailure { _login.value = LoginUiState(error = it.message ?: "تعذر تسجيل الدخول.") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            _state.value = AuthState.SignedOut
        }
    }
}
