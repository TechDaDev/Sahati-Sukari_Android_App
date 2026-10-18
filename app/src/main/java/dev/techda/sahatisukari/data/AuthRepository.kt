package dev.techda.sahatisukari.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
