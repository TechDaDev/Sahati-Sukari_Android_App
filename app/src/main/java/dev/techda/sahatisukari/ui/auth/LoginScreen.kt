package dev.techda.sahatisukari.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.techda.sahatisukari.data.RegisterRequest

@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel(), onEmergency: () -> Unit = {}) {
    val flow by viewModel.flow.collectAsState()
    val form by viewModel.form.collectAsState()
    when (val screen = flow) {
        PublicFlow.Login -> LoginForm(form, viewModel, onEmergency)
        PublicFlow.Register -> RegistrationForm(form, viewModel)
        is PublicFlow.Activation -> OtpForm("تفعيل البريد الإلكتروني", screen.email, form, onSubmit = viewModel::verifyActivation, onResend = viewModel::resendActivation, onBack = viewModel::showLogin)
        PublicFlow.ForgotPassword -> EmailForm("استعادة كلمة المرور", "أدخل بريدك الإلكتروني لإرسال رمز التحقق.", form, onSubmit = viewModel::requestPasswordReset, onBack = viewModel::showLogin)
        is PublicFlow.ResetOtp -> OtpForm("التحقق من الهوية", screen.email, form, onSubmit = viewModel::verifyPasswordReset, onBack = viewModel::showLogin)
        is PublicFlow.NewPassword -> NewPasswordForm(form, viewModel::completePasswordReset)
    }
}

@Composable
private fun LoginForm(form: FormState, viewModel: AuthViewModel, onEmergency: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthPage("صحتي وسكري", "سجّل الدخول إلى حسابك") {
        form.notice?.let { Message(it, false) }
        Input(email, { email = it }, "البريد الإلكتروني", KeyboardType.Email)
        Input(password, { password = it }, "كلمة المرور", KeyboardType.Password, secret = true)
        form.error?.let { Message(it, true) }
        PrimaryAction("تسجيل الدخول", form.busy, enabled = email.isNotBlank() && password.isNotEmpty()) { viewModel.login(email, password) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = viewModel::showRegistration) { Text("إنشاء حساب") }
            TextButton(onClick = viewModel::showForgotPassword) { Text("نسيت كلمة المرور؟") }
        }
        TextButton(onClick = onEmergency, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("مساعدة عاجلة — 911") }
    }
}

@Composable
private fun RegistrationForm(form: FormState, viewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("FEMALE") }
    var birth by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    AuthPage("إنشاء حساب مريض", "استخدم بياناتك الصحيحة لإنشاء الحساب") {
        Input(name, { name = it }, "الاسم الكامل")
        Text("الجنس", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = gender == "FEMALE", onClick = { gender = "FEMALE" }); Text("أنثى")
            RadioButton(selected = gender == "MALE", onClick = { gender = "MALE" }); Text("ذكر")
        }
        Input(birth, { birth = it }, "تاريخ الميلاد (YYYY-MM-DD)")
        Input(height, { height = it.filter(Char::isDigit) }, "الطول بالسنتيمتر", KeyboardType.Number)
        Input(nationalId, { nationalId = it }, "الرقم الوطني", KeyboardType.Number, secret = false)
        Input(email, { email = it }, "البريد الإلكتروني", KeyboardType.Email)
        Input(phone, { phone = it }, "رقم الهاتف", KeyboardType.Phone)
        Input(password, { password = it }, "كلمة المرور", KeyboardType.Password, secret = true)
        Text("استخدم 10 أحرف على الأقل، مع حرف إنجليزي كبير وصغير ورقم.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Input(confirm, { confirm = it }, "تأكيد كلمة المرور", KeyboardType.Password, secret = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(consent, { consent = it })
            Text("أوافق على استخدام الرقم الوطني للتحقق من الهوية.", style = MaterialTheme.typography.bodySmall)
        }
        form.error?.let { Message(it, true) }
        val ready = name.isNotBlank() && birth.isNotBlank() && height.isNotBlank() && nationalId.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && password.isNotEmpty() && confirm.isNotEmpty() && consent
        PrimaryAction("إنشاء الحساب", form.busy, ready) {
            viewModel.register(RegisterRequest(name, gender, birth, height.toIntOrNull() ?: 0, nationalId, email, phone, password, confirm, consent))
        }
        TextButton(onClick = viewModel::showLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("العودة لتسجيل الدخول") }
    }
}

@Composable
private fun EmailForm(title: String, description: String, form: FormState, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    AuthPage(title, description) {
        Input(email, { email = it }, "البريد الإلكتروني", KeyboardType.Email)
        form.notice?.let { Message(it, false) }
        form.error?.let { Message(it, true) }
        PrimaryAction("إرسال رمز التحقق", form.busy, email.isNotBlank()) { onSubmit(email) }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("عودة") }
    }
}

@Composable
private fun OtpForm(title: String, email: String, form: FormState, onSubmit: (String) -> Unit, onResend: (() -> Unit)? = null, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    AuthPage(title, "أدخل الرمز المكوّن من ستة أرقام المرسل إلى $email") {
        Input(code, { code = it.filter(Char::isDigit).take(6) }, "رمز التحقق", KeyboardType.Number)
        form.notice?.let { Message(it, false) }
        form.error?.let { Message(it, true) }
        PrimaryAction("تحقق", form.busy, code.length == 6) { onSubmit(code) }
        if (onResend != null) TextButton(onClick = onResend, enabled = !form.busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("إعادة إرسال الرمز") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("عودة") }
    }
}

@Composable
private fun NewPasswordForm(form: FormState, onSubmit: (String, String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AuthPage("تعيين كلمة مرور جديدة", "اختر كلمة مرور قوية لحسابك") {
        Input(password, { password = it }, "كلمة المرور الجديدة", KeyboardType.Password, secret = true)
        Input(confirm, { confirm = it }, "تأكيد كلمة المرور", KeyboardType.Password, secret = true)
        form.error?.let { Message(it, true) }
        PrimaryAction("تحديث كلمة المرور", form.busy, password.isNotEmpty() && confirm.isNotEmpty()) { onSubmit(password, confirm) }
    }
}

@Composable
private fun AuthPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                androidx.compose.foundation.layout.Box(Modifier.padding(18.dp)) { androidx.compose.material3.Icon(Icons.Outlined.Bloodtype, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Text(title, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(subtitle, Modifier.padding(top = 8.dp, bottom = 22.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            content()
        }
    }
}

@Composable
private fun ColumnScope.Input(value: String, onValue: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, secret: Boolean = false) {
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth().padding(bottom = 10.dp), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard), visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
}

@Composable
private fun ColumnScope.Message(message: String, error: Boolean) {
    Text(message, Modifier.fillMaxWidth().padding(bottom = 10.dp), color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
}

@Composable
private fun ColumnScope.PrimaryAction(label: String, busy: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
        if (busy) CircularProgressIndicator(Modifier.padding(vertical = 3.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(label)
    }
}
