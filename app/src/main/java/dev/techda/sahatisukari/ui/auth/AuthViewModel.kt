package dev.techda.sahatisukari.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.techda.sahatisukari.data.AuthRepository
import dev.techda.sahatisukari.data.AuthUser
import dev.techda.sahatisukari.data.RegisterRequest
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

sealed interface PublicFlow {
    data object Login : PublicFlow
    data object Register : PublicFlow
    data class Activation(val email: String) : PublicFlow
    data object ForgotPassword : PublicFlow
    data class ResetOtp(val email: String) : PublicFlow
    data class NewPassword(val authorization: String) : PublicFlow
}

data class FormState(val busy: Boolean = false, val error: String? = null, val notice: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _flow = MutableStateFlow<PublicFlow>(PublicFlow.Login)
    val flow: StateFlow<PublicFlow> = _flow.asStateFlow()
    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = runCatching { repository.restoreSession() }.getOrNull()?.let(AuthState::SignedIn) ?: AuthState.SignedOut
        }
    }

    fun showLogin(notice: String? = null) { _flow.value = PublicFlow.Login; _form.value = FormState(notice = notice) }
    fun showRegistration() { _flow.value = PublicFlow.Register; _form.value = FormState() }
    fun showForgotPassword() { _flow.value = PublicFlow.ForgotPassword; _form.value = FormState() }

    fun login(email: String, password: String) = submit {
        val user = repository.login(email, password)
        _state.value = AuthState.SignedIn(user)
        _form.value = FormState()
    }

    fun register(body: RegisterRequest) = submit {
        val activationRequired = repository.register(body)
        if (activationRequired) {
            _flow.value = PublicFlow.Activation(body.email)
            _form.value = FormState(notice = "إذا كان الحساب مؤهلًا، أُرسل رمز التفعيل إلى البريد المسجّل.")
        } else {
            val user = repository.restoreSession() ?: error("تم إنشاء الحساب. سجّل الدخول للمتابعة.")
            _state.value = AuthState.SignedIn(user)
            _form.value = FormState()
        }
    }

    fun verifyActivation(code: String) {
        val email = (_flow.value as? PublicFlow.Activation)?.email ?: return
        submit {
            repository.verifyActivation(email, code)
            showLogin("تم تفعيل الحساب. سجّل الدخول للمتابعة.")
        }
    }

    fun resendActivation() {
        val email = (_flow.value as? PublicFlow.Activation)?.email ?: return
        submit {
            repository.resendActivation(email)
            _form.value = FormState(notice = "إذا كان الحساب مؤهلًا، سيصل رمز جديد إلى البريد المسجّل.")
        }
    }

    fun requestPasswordReset(email: String) = submit {
        repository.requestPasswordReset(email)
        _flow.value = PublicFlow.ResetOtp(email)
        _form.value = FormState(notice = "إذا كان البريد مؤهلًا، فسيصل رمز التحقق إليه.")
    }

    fun verifyPasswordReset(code: String) {
        val email = (_flow.value as? PublicFlow.ResetOtp)?.email ?: return
        submit {
            val authorization = repository.verifyPasswordReset(email, code)
            _flow.value = PublicFlow.NewPassword(authorization)
            _form.value = FormState()
        }
    }

    fun completePasswordReset(newPassword: String, confirmPassword: String) {
        val authorization = (_flow.value as? PublicFlow.NewPassword)?.authorization ?: return
        submit {
            repository.completePasswordReset(authorization, newPassword, confirmPassword)
            showLogin("تم تحديث كلمة المرور. سجّل الدخول مجددًا.")
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            _state.value = AuthState.SignedOut
            showLogin()
        }
    }

    private fun submit(action: suspend () -> Unit) {
        if (_form.value.busy) return
        _form.value = FormState(busy = true)
        viewModelScope.launch {
            runCatching { action() }.onFailure { _form.value = FormState(error = it.message ?: "تعذر إكمال الطلب.") }
        }
    }
}
