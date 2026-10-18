package dev.techda.sahatisukari.data

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.HTTP
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.QueryMap
import okhttp3.ResponseBody

interface AuthApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<TokenResponse>>
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): Response<ApiEnvelope<JsonObject>>
    @POST("auth/logout") suspend fun logout(): Response<ApiEnvelope<JsonObject>>
    @POST("auth/logout-all") suspend fun logoutAll(): Response<ApiEnvelope<JsonObject>>
    @POST("auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiEnvelope<JsonObject>>
    @GET("auth/sessions") suspend fun sessions(): Response<ApiEnvelope<JsonObject>>
    @HTTP(method = "DELETE", path = "auth/sessions/{id}", hasBody = false) suspend fun deleteSession(@Path("id") id: String): Response<ApiEnvelope<JsonObject>>
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
    @PATCH("profile") suspend fun updateProfile(@Body body: JsonObject): Response<ApiEnvelope<JsonObject>>
    @GET("profile/{section}") suspend fun profileSection(@Path("section") section: String): Response<ApiEnvelope<JsonObject>>
    @PUT("profile/{section}") suspend fun updateProfileSection(@Path("section") section: String, @Body body: JsonObject): Response<ApiEnvelope<JsonObject>>
    @GET("{collection}") suspend fun list(@Path("collection") collection: String, @QueryMap query: Map<String, String> = emptyMap()): Response<ApiEnvelope<JsonObject>>
    @POST("{collection}") suspend fun create(@Path("collection") collection: String, @Body body: JsonObject): Response<ApiEnvelope<JsonObject>>
    @PATCH("{collection}/{id}") suspend fun update(@Path("collection") collection: String, @Path("id") id: String, @Body body: JsonObject): Response<ApiEnvelope<JsonObject>>
    @HTTP(method = "DELETE", path = "{collection}/{id}", hasBody = false) suspend fun delete(@Path("collection") collection: String, @Path("id") id: String): Response<ApiEnvelope<JsonObject>>
    @PATCH("settings/contact") suspend fun updateContact(@Body body: JsonObject): Response<ApiEnvelope<JsonObject>>
    @GET("reports/summary") suspend fun reportSummary(@QueryMap query: Map<String, String> = emptyMap()): Response<ApiEnvelope<JsonObject>>
    @GET("reports/pdf") suspend fun reportPdf(@QueryMap query: Map<String, String> = emptyMap()): retrofit2.Response<ResponseBody>
}
