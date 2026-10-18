package dev.techda.sahatisukari.data

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.HTTP

interface AuthApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<TokenResponse>>
    @POST("auth/logout") suspend fun logout(): Response<ApiEnvelope<JsonObject>>
    @POST("auth/logout-all") suspend fun logoutAll(): Response<ApiEnvelope<JsonObject>>
    @POST("auth/refresh") suspend fun refresh(@Body body: RefreshRequest, @retrofit2.http.Header("X-Refresh-Request-ID") requestId: String): Response<ApiEnvelope<TokenResponse>>
    @GET("auth/me") suspend fun me(): Response<ApiEnvelope<CurrentUserResponse>>
    @POST("auth/activation/verify") suspend fun verifyActivation(@Body body: OtpRequest): Response<ApiEnvelope<JsonObject>>
    @POST("auth/activation/resend") suspend fun resendActivation(@Body body: EmailRequest): Response<ApiEnvelope<JsonObject>>
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: EmailRequest): Response<ApiEnvelope<ForgotPasswordResponse>>
    @POST("auth/password-reset/verify") suspend fun verifyPasswordReset(@Body body: OtpRequest): Response<ApiEnvelope<JsonObject>>
    @POST("auth/password-reset/complete") suspend fun completePasswordReset(@Body body: ResetPasswordRequest): Response<ApiEnvelope<JsonObject>>
}

interface PatientApi {
    @GET("dashboard") suspend fun dashboard(): Response<ApiEnvelope<JsonObject>>
    @GET("profile") suspend fun profile(): Response<ApiEnvelope<JsonObject>>
    @GET("{collection}") suspend fun list(@Path("collection") collection: String): Response<ApiEnvelope<JsonObject>>
}
