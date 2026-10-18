package dev.techda.sahatisukari.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class PatientApiContractTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var api: PatientApi

    @Before fun setUp() {
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("api/v1/")).client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(PatientApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun glucoseCreateUsesDocumentedPathAndSchemaFields() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"ok":true,"data":{"id":"synthetic-glucose-id"}}"""))
        val body = buildJsonObject {
            put("glucoseValue", 126.5); put("glucoseUnit", "MG_DL"); put("measurementDate", "2026-10-18")
            put("measurementTime", "08:15"); put("measurementContext", "FASTING"); put("source", "FINGERSTICK_METER")
        }
        val response = api.create("glucose", body)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/glucose", request.path)
        val sent = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("MG_DL", sent["glucoseUnit"]?.jsonPrimitive?.content)
        assertEquals("FASTING", sent["measurementContext"]?.jsonPrimitive?.content)
        assertEquals(201, response.code())
    }

    @Test fun recordListsUseCursorCompatibleQueryParameters() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true,"data":{"items":[],"pagination":{"hasMore":false,"nextCursor":null}}}"""))
        val response = api.list("glucose", mapOf("limit" to "50", "sort" to "desc", "cursor" to "next-token"))
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/api/v1/glucose?"))
        assertTrue(request.path!!.contains("limit=50"))
        assertTrue(request.path!!.contains("cursor=next-token"))
        assertEquals(200, response.code())
    }

    @Test fun deleteUsesScopedRecordIdPath() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true,"data":{}}"""))
        api.delete("glucose", "synthetic-id")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/glucose/synthetic-id", request.path)
    }
}
