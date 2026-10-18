package dev.techda.sahatisukari.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class AuthApiContractTest {
    private val server = MockWebServer()
    private lateinit var api: AuthApi

    @Before fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("api/v1/"))
            .client(OkHttpClient())
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(AuthApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun loginUsesDocumentedPayloadAndReadsRotatableSession() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""
            {"ok":true,"data":{"accessToken":"access-test","refreshToken":"refresh-test","tokenType":"Bearer","accessExpiresAt":"2026-10-18T12:00:00.000Z","refreshExpiresAt":"2026-11-17T12:00:00.000Z","user":{"id":"synthetic-id","email":"patient@example.test","role":"PATIENT","mustChangePassword":false}}}
        """.trimIndent()))

        val response = kotlinx.coroutines.runBlocking {
            api.login(LoginRequest("patient@example.test", "NotARealPassword123", "Android test"))
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/login", request.path)
        assertEquals("patient@example.test", request.body.readUtf8().let { Json.parseToJsonElement(it).jsonObject["email"]!!.jsonPrimitive.content })
        assertEquals(201, response.code())
        assertNotNull(response.body()?.data?.accessExpiresAt)
        assertEquals("PATIENT", response.body()?.data?.user?.role)
    }

    @Test fun registrationUsesCurrentBackendSchema() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"ok":true,"data":{"activationRequired":true}}"""))
        kotlinx.coroutines.runBlocking {
            api.register(RegisterRequest("Synthetic Patient", "FEMALE", "1990-01-01", 165, "SYNTHETIC-ID", "patient@example.test", "+9647000000000", "SyntheticPassword123", "SyntheticPassword123", true))
        }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/register", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("FEMALE", body["gender"]!!.jsonPrimitive.content)
        assertEquals("SYNTHETIC-ID", body["nationalId"]!!.jsonPrimitive.content)
        assertEquals("true", body["nationalIdConsent"]!!.jsonPrimitive.content)
    }

    @Test fun refreshSendsIdempotencyHeaderAndRecoveryUsesResetContract() {
        server.enqueue(MockResponse().setBody("""{"ok":true,"data":{"accessToken":"new-access","refreshToken":"new-refresh","accessExpiresAt":"2026-10-18T12:00:00Z","refreshExpiresAt":"2026-11-17T12:00:00Z","user":{"id":"synthetic-id","email":"patient@example.test","role":"PATIENT"}}}"""))
        kotlinx.coroutines.runBlocking { api.refresh(RefreshRequest("old-refresh"), "00000000-0000-4000-8000-000000000001") }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/refresh", request.path)
        assertEquals("00000000-0000-4000-8000-000000000001", request.getHeader("X-Refresh-Request-ID"))
        server.enqueue(MockResponse().setBody("""{"ok":true,"data":{"message":"If eligible, a reset code was sent."}}"""))
        kotlinx.coroutines.runBlocking { api.forgotPassword(EmailRequest("patient@example.test")) }
        val recovery = server.takeRequest()
        assertEquals("/api/v1/auth/forgot-password", recovery.path)
        assertEquals("patient@example.test", Json.parseToJsonElement(recovery.body.readUtf8()).jsonObject["email"]!!.jsonPrimitive.content)
    }
}
