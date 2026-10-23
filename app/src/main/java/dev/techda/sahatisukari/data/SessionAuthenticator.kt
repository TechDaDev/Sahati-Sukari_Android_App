package dev.techda.sahatisukari.data

import dev.techda.sahatisukari.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.UUID
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SessionAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @Named("bare") private val bareClient: okhttp3.OkHttpClient,
) : Authenticator {
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? = synchronized(lock) {
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return@synchronized null
        val prior = response.request.header("Authorization")?.removePrefix("Bearer ")
        val current = runBlocking { tokenStore.read() } ?: return@synchronized null
        if (prior != current.accessToken) return@synchronized response.request.newBuilder().header("Authorization", "Bearer ${current.accessToken}").build()
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(current.refreshToken))
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "auth/refresh")
            .header("Content-Type", "application/json")
            .header("X-Refresh-Request-ID", UUID.randomUUID().toString())
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        var refreshTokenRejected = false
        val refreshed = try {
            bareClient.newCall(request).execute().use { result ->
                if (!result.isSuccessful) {
                    refreshTokenRejected = result.code == 401 || result.code == 403
                    null
                }
                else {
                    val text = result.body?.string() ?: return@use null
                    val data = json.parseToJsonElement(text).jsonObject["data"]?.jsonObject ?: return@use null
                    TokenResponse(
                        accessToken = data["accessToken"]!!.jsonPrimitive.content,
                        refreshToken = data["refreshToken"]!!.jsonPrimitive.content,
                        accessExpiresAt = data["accessExpiresAt"]!!.jsonPrimitive.content,
                        refreshExpiresAt = data["refreshExpiresAt"]!!.jsonPrimitive.content,
                    )
                }
            }
        } catch (_: IOException) {
            return@synchronized null
        } catch (_: Exception) {
            return@synchronized null
        }
        if (refreshed == null) {
            if (refreshTokenRejected) runBlocking { tokenStore.clear() }
            return@synchronized null
        }
        runBlocking { tokenStore.save(SessionTokens(refreshed.accessToken, refreshed.refreshToken)) }
        response.request.newBuilder().header("Authorization", "Bearer ${refreshed.accessToken}").build()
    }
}

