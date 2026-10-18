package dev.techda.sahatisukari.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val login by viewModel.login.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                androidx.compose.foundation.layout.Box(Modifier.padding(18.dp)) {
                    androidx.compose.material3.Icon(Icons.Outlined.Bloodtype, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("صحتي وسكري", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("سجّل الدخول إلى حسابك", Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            login.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { onLogin(email, password) }, enabled = !login.busy && email.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                if (login.busy) CircularProgressIndicator(Modifier.padding(vertical = 4.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("تسجيل الدخول")
            }
            Text("للمساعدة في الوصول إلى حسابك، تواصل مع الدعم.", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
