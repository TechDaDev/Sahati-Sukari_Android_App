package dev.techda.sahatisukari.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
) {
    companion object { val shared = UpdateRepository() }

    private val mutex = Mutex()
    private var cached: UpdateCheck? = null
    private var lastFailureAtMillis = 0L
    private val backoffMillis = 60_000L

    suspend fun check(installedVersionCode: Int): UpdateCheck = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastFailureAtMillis in 0 until backoffMillis) {
                return@withLock cached?.let { it.copy(available = it.manifest.versionCode > installedVersionCode) }
                    ?: error("تعذر الاتصال بخدمة التحديث. حاول لاحقًا.")
            }
            try {
                val request = Request.Builder().url(UpdateManifestValidator.MANIFEST_URL).get().build()
                client.newCall(request).execute().use { response ->
                    require(response.isSuccessful) { "تعذر التحقق من التحديثات (HTTP ${response.code})." }
                    val body = response.body?.string() ?: error("ملف التحديث فارغ.")
                    val manifest = UpdateManifestValidator.parseAndValidate(body)
                    UpdateCheck(manifest, manifest.versionCode > installedVersionCode, now).also { cached = it }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastFailureAtMillis = now
                cached?.let { it.copy(available = it.manifest.versionCode > installedVersionCode) } ?: throw error
            }
        }
    }
}

data class UpdateCheck(val manifest: UpdateManifest, val available: Boolean, val checkedAtMillis: Long = System.currentTimeMillis())
