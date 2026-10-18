package dev.techda.sahatisukari.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(val ok: Boolean, val data: T? = null, val error: String? = null, val code: String? = null, val fields: Map<String, String> = emptyMap())

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceLabel: String = "Android")

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
    val user: AuthUser? = null,
)

@Serializable
data class AuthUser(val id: String, val email: String, val role: String, val mustChangePassword: Boolean = false)

@Serializable
data class CurrentUserResponse(val user: AuthUser)

@Serializable
data class OtpRequest(val email: String, val code: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class ForgotPasswordResponse(val message: String)

@Serializable
data class ResetPasswordRequest(val resetAuthorization: String, val newPassword: String, val confirmPassword: String)
