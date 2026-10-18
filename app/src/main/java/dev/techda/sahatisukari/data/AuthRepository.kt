package dev.techda.sahatisukari.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    @Named("session") private val sessionApi: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    suspend fun restoreSession(): AuthUser? {
        if (tokenStore.read() == null) return null
        val response = sessionApi.me()
        if (!response.isSuccessful) {
            tokenStore.clear()
            return null
        }
        return response.body()?.data?.user ?: run { tokenStore.clear(); null }
    }

    suspend fun login(email: String, password: String): AuthUser {
        val response = authApi.login(LoginRequest(email.trim(), password, "Android"))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
        val payload = response.body()?.data ?: throw ApiFailure("تعذر قراءة استجابة الخادم.")
        if (payload.user?.role != "PATIENT") throw ApiFailure("هذا التطبيق مخصص لحسابات المرضى.")
        tokenStore.save(SessionTokens(payload.accessToken, payload.refreshToken))
        return payload.user
    }

    suspend fun register(body: RegisterRequest): Boolean {
        val response = authApi.register(body)
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
        val payload = response.body()?.data ?: throw ApiFailure("تعذر قراءة استجابة الخادم.")
        val activationRequired = payload["activationRequired"]?.jsonPrimitive?.booleanOrNull == true
        if (activationRequired) return true
        val tokenResponse = runCatching { json.decodeFromJsonElement<TokenResponse>(payload) }.getOrNull()
            ?: throw ApiFailure("تم إنشاء الحساب. سجّل الدخول للمتابعة.")
        val user = tokenResponse.user ?: throw ApiFailure("تم إنشاء الحساب. سجّل الدخول للمتابعة.")
        if (user.role != "PATIENT") throw ApiFailure("هذا التطبيق مخصص لحسابات المرضى.")
        tokenStore.save(SessionTokens(tokenResponse.accessToken, tokenResponse.refreshToken))
        return false
    }

    suspend fun verifyActivation(email: String, code: String) {
        val response = authApi.verifyActivation(OtpRequest(email, code))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
    }

    suspend fun resendActivation(email: String) {
        val response = authApi.resendActivation(EmailRequest(email))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
    }

    suspend fun requestPasswordReset(email: String) {
        val response = authApi.forgotPassword(EmailRequest(email))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
    }

    suspend fun verifyPasswordReset(email: String, code: String): String {
        val response = authApi.verifyPasswordReset(OtpRequest(email, code))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
        return response.body()?.data?.get("resetAuthorization")?.jsonPrimitive?.content
            ?: throw ApiFailure("تعذر التحقق من الرمز.")
    }

    suspend fun completePasswordReset(authorization: String, newPassword: String, confirmPassword: String) {
        val response = authApi.completePasswordReset(ResetPasswordRequest(authorization, newPassword, confirmPassword))
        if (!response.isSuccessful) throw ApiFailure(errorMessage(response.code(), response.errorBody()?.string()))
    }

    suspend fun logout() {
        try { sessionApi.logout() } finally { tokenStore.clear() }
    }

    private fun errorMessage(status: Int, body: String?): String {
        val serverMessage = runCatching {
            json.parseToJsonElement(body.orEmpty()).jsonObject["error"]?.jsonPrimitive?.content
        }.getOrNull()
        if (!serverMessage.isNullOrBlank()) return serverMessage
        return when (status) {
            401 -> "البريد الإلكتروني أو كلمة المرور غير صحيحة."
            403 -> "تعذر تسجيل الدخول. تحقق من تفعيل الحساب."
            429 -> "محاولات كثيرة. حاول لاحقًا."
            in 500..599 -> "الخدمة غير متاحة مؤقتًا. حاول مرة أخرى."
            else -> "تعذر إكمال الطلب. تحقق من البيانات وحاول مرة أخرى."
        }
    }
}

class ApiFailure(message: String) : Exception(message)
