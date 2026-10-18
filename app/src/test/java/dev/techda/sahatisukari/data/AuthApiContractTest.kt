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
}
