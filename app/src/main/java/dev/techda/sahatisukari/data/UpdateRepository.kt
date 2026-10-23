package dev.techda.sahatisukari.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateRepository internal constructor(
    private val client: OkHttpClient,
    private val apkUrl: HttpUrl,
) {
    constructor() : this(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .followRedirects(false)
            .followSslRedirects(false)
            .build(),
        APK_URL.toHttpUrl(),
    )

    companion object {
        const val APK_URL = "https://download.techda.dev/SahatiSukari.apk"
        val shared = UpdateRepository()
        private const val MAX_APK_BYTES = 100L * 1024 * 1024
        private const val PREFS = "verified_apk_update"
        private const val KEY_FINGERPRINT = "fingerprint"
        private const val KEY_VERSION_CODE = "version_code"
        private const val KEY_VERSION_NAME = "version_name"
        private const val KEY_CHECKED_AT = "checked_at"
        private const val APK_FILE = "SahatiSukari-update.apk"

        fun installedVersionCode(context: Context): Long {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }
    }

    private val mutex = Mutex()

    /** Headers only identify a possible new file. A candidate is an update only after PackageManager validates its APK. */
    suspend fun check(
        context: Context,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): UpdateCheck = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            val installedCode = installedVersionCode(appContext)
            val directory = File(appContext.cacheDir, "updates")
            val staged = File(directory, APK_FILE)
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val probe = probe()
            val savedFingerprint = prefs.getString(KEY_FINGERPRINT, null)
            val savedCode = prefs.getLong(KEY_VERSION_CODE, 0L)
            val savedName = prefs.getString(KEY_VERSION_NAME, null)

            if (probe.fingerprint != null && probe.fingerprint == savedFingerprint && savedCode > 0L && !savedName.isNullOrBlank()) {
                val available = UpdateVersionPolicy.isNewer(savedCode, installedCode)
                if (!available) staged.delete()
                return@withLock UpdateCheck(
                    installedVersionCode = installedCode,
                    availableVersionCode = savedCode,
                    availableVersionName = savedName,
                    available = available,
                    stagedApkPath = staged.takeIf { available && it.isFile && it.length() > 0 }?.absolutePath,
                    checkedAtMillis = System.currentTimeMillis(),
                    fingerprint = probe.fingerprint,
                )
            }

            if (probe.fingerprint != savedFingerprint) staged.delete()
            directory.mkdirs()
            val candidate = download(probe, staged, onProgress)
            try {
                val verified = UpdateApkInstaller.inspectAndValidate(appContext, candidate)
                prefs.edit()
                    .putString(KEY_FINGERPRINT, probe.fingerprint ?: candidateFingerprint(candidate))
                    .putLong(KEY_VERSION_CODE, verified.versionCode)
                    .putString(KEY_VERSION_NAME, verified.versionName)
                    .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                    .apply()
                val available = UpdateVersionPolicy.isNewer(verified.versionCode, installedCode)
                if (!available) candidate.delete()
                UpdateCheck(
                    installedVersionCode = installedCode,
                    availableVersionCode = verified.versionCode,
                    availableVersionName = verified.versionName,
                    available = available,
                    stagedApkPath = candidate.takeIf { available && it.isFile }?.absolutePath,
                    checkedAtMillis = System.currentTimeMillis(),
                    fingerprint = probe.fingerprint,
                )
            } catch (error: Exception) {
                candidate.delete()
                throw error
            }
        }
    }

    suspend fun apkForInstall(
        context: Context,
        expectedVersionCode: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): File = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val directory = File(appContext.cacheDir, "updates")
        val staged = File(directory, APK_FILE)
        if (staged.isFile && staged.length() > 0L) {
            val verified = UpdateApkInstaller.inspectAndValidate(appContext, staged)
            if (verified.versionCode == expectedVersionCode && expectedVersionCode > installedVersionCode(appContext)) return@withContext staged
            staged.delete()
        }

        val current = probe()
        directory.mkdirs()
        val file = download(current, staged, onProgress)
        try {
            val verified = UpdateApkInstaller.inspectAndValidate(appContext, file)
            require(verified.versionCode == expectedVersionCode) { "تغيّر الإصدار المنشور. أعد التحقق من التحديثات." }
            require(verified.versionCode > installedVersionCode(appContext)) { "أنت تستخدم أحدث إصدار من التطبيق." }
            file
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    private fun probe(): RemoteApk {
        val request = Request.Builder().url(apkUrl).header("Cache-Control", "no-cache").head().build()
        client.newCall(request).execute().use { response ->
            if (response.code == 405 || response.code == 501) return RemoteApk(fingerprint = null)
            require(response.code == 200) { "تعذّر التحقق من وجود تحديثات (HTTP ${response.code})." }
            validateResponseOrigin(response)
            validateContentType(response.header("Content-Type"))
            val size = response.header("Content-Length")?.toLongOrNull()
            require(size == null || size in 1L..MAX_APK_BYTES) { "حجم ملف التحديث غير صالح." }
            val fingerprint = responseFingerprint(response)
            return RemoteApk(fingerprint = fingerprint, expectedLength = size)
        }
    }

    private suspend fun download(
        remote: RemoteApk,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File {
        require(destination.parentFile?.exists() == true || destination.parentFile?.mkdirs() == true) { "تعذّر تجهيز مساحة التحديث." }
        val temporary = File(destination.parentFile, "${destination.name}.part")
        val request = Request.Builder().url(apkUrl).header("Cache-Control", "no-cache").get().build()
        try {
            client.newCall(request).execute().use { response ->
                require(response.code == 200) { "تعذّر تنزيل التحديث (HTTP ${response.code})." }
                validateResponseOrigin(response)
                validateContentType(response.header("Content-Type"))
                val body = response.body ?: error("ملف التحديث فارغ.")
                val contentLength = body.contentLength()
                require(contentLength == -1L || contentLength in 1L..MAX_APK_BYTES) { "حجم ملف التحديث غير صالح." }
                if (remote.expectedLength != null && contentLength >= 0L) {
                    require(contentLength == remote.expectedLength) { "تغيّر ملف APK أثناء التحقق. أعد المحاولة." }
                }
                if (remote.fingerprint != null) {
                    val downloadedFingerprint = responseFingerprint(response)
                    require(fingerprintsAgree(remote.fingerprint, downloadedFingerprint)) { "تغيّر ملف APK أثناء التحقق. أعد المحاولة." }
                }
                var written = 0L
                body.byteStream().use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            require(written <= MAX_APK_BYTES) { "حجم ملف التحديث أكبر من المسموح." }
                            output.write(buffer, 0, read)
                            onProgress(written, contentLength.takeIf { it > 0 } ?: remote.expectedLength ?: -1L)
                        }
                        output.fd.sync()
                    }
                }
                require(written > 0L && (contentLength < 0L || written == contentLength)) { "اكتمل تنزيل APK بصورة غير صحيحة." }
            }
            if (destination.exists() && !destination.delete()) error("تعذّر استبدال ملف التحديث السابق.")
            check(temporary.renameTo(destination)) { "تعذّر حفظ ملف التحديث." }
            return destination
        } catch (error: Exception) {
            temporary.delete()
            if (error is CancellationException) throw error
            throw error
        }
    }

    private fun validateResponseOrigin(response: Response) {
        val url = response.request.url
        require(UpdatePolicy.isTrustedApkUrl(url.toString())) {
            "رابط تحديث غير موثوق."
        }
    }

    private fun validateContentType(value: String?) {
        require(UpdatePolicy.isSupportedContentType(value)) {
            "نوع ملف التحديث غير مدعوم."
        }
    }

    private fun responseFingerprint(response: Response): String? {
        val values = listOf(
            response.header("ETag")?.trim(),
            response.header("Last-Modified")?.trim(),
            response.header("Content-Length")?.trim(),
            response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase(),
        )
        if (values.take(3).all { it.isNullOrBlank() }) return null
        return values.joinToString("|") { it.orEmpty() }
    }

    private fun fingerprintsAgree(expected: String, actual: String?): Boolean {
        if (actual == null) return false
        val expectedParts = expected.split('|')
        val actualParts = actual.split('|')
        return expectedParts.indices.all { index ->
            expectedParts[index].isBlank() || actualParts.getOrNull(index).isNullOrBlank() || expectedParts[index] == actualParts[index]
        }
    }

    private fun candidateFingerprint(file: File): String = "unfingerprinted:${file.length()}:${file.lastModified()}"
}

private data class RemoteApk(
    val fingerprint: String?,
    val expectedLength: Long? = null,
)

internal object UpdateVersionPolicy {
    fun isNewer(candidateVersionCode: Long, installedVersionCode: Long): Boolean = candidateVersionCode > installedVersionCode
}

internal object UpdatePolicy {
    fun isTrustedApkUrl(value: String): Boolean = runCatching {
        val url = value.toHttpUrl()
        url.isHttps && url.host.equals("download.techda.dev", ignoreCase = true) &&
            url.port == 443 && url.encodedPath == "/SahatiSukari.apk" && url.query == null && url.fragment == null && url.username.isEmpty() && url.password.isEmpty()
    }.getOrDefault(false)

    fun isSupportedContentType(value: String?): Boolean {
        val type = value?.substringBefore(';')?.trim()?.lowercase()
        return type == "application/vnd.android.package-archive" || type == "application/octet-stream"
    }
}

data class UpdateCheck(
    val installedVersionCode: Long,
    val availableVersionCode: Long,
    val availableVersionName: String,
    val available: Boolean,
    val stagedApkPath: String?,
    val checkedAtMillis: Long = System.currentTimeMillis(),
    val fingerprint: String?,
)
