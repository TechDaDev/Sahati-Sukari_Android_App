package dev.techda.sahatisukari.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UpdateRepository internal constructor(
    private val client: OkHttpClient,
    private val manifestUrl: HttpUrl,
) {
    constructor() : this(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .followRedirects(false)
            .followSslRedirects(false)
            .build(),
        UpdateManifestValidator.MANIFEST_URL.toHttpUrl(),
    )

    companion object {
        val shared = UpdateRepository()
        private const val MAX_APK_BYTES = 100L * 1024 * 1024
        private const val MAX_MANIFEST_BYTES = 64L * 1024
        private const val CHECK_COOLDOWN_MS = 6L * 60 * 60 * 1000
        private const val PREFS = "verified_apk_update"
        private const val KEY_MANIFEST = "manifest_json"
        private const val KEY_CHECKED_AT = "checked_at"
        private const val APK_FILE_PREFIX = "SahatiSukari-"

        fun installedVersionCode(context: Context): Long {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }
    }

    private val checkMutex = Mutex()
    private val downloadMutex = Mutex()

    /** Fetches only the small manifest. APK bytes are downloaded only after the user chooses Update. */
    suspend fun check(context: Context, forceRefresh: Boolean = false): UpdateCheck = withContext(Dispatchers.IO) {
        checkMutex.withLock {
            val appContext = context.applicationContext
            val installedCode = installedVersionCode(appContext)
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val cachedJson = prefs.getString(KEY_MANIFEST, null)
            val cachedAt = prefs.getLong(KEY_CHECKED_AT, 0L)
            val cached = cachedJson?.let { runCatching { UpdateManifestValidator.parseAndValidate(it) }.getOrNull() }
            val cacheIsFresh = cached != null && now - cachedAt in 0..CHECK_COOLDOWN_MS

            val (manifest, checkedAt) = if (!forceRefresh && cacheIsFresh) {
                cached!! to cachedAt
            } else {
                val fetched = fetchManifest()
                prefs.edit().putString(KEY_MANIFEST, fetched.first).putLong(KEY_CHECKED_AT, now).apply()
                fetched.second to now
            }
            UpdateCheck(
                installedVersionCode = installedCode,
                manifest = manifest,
                available = UpdateVersionPolicy.isNewer(manifest.versionCode, installedCode),
                required = UpdateVersionPolicy.isRequired(installedCode, manifest.minimumSupportedVersionCode),
                checkedAtMillis = checkedAt,
            )
        }
    }

    suspend fun apkForInstall(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): File = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            var stage = "manifest_validation"
            var temporaryFile: File? = null
            try {
                val validatedManifest = UpdateManifestValidator.validate(manifest)
                val appContext = context.applicationContext
                require(validatedManifest.versionCode > installedVersionCode(appContext)) { "أنت تستخدم أحدث إصدار من التطبيق." }
                val directory = File(appContext.cacheDir, "updates")
                check(directory.exists() || directory.mkdirs()) { "تعذّر تجهيز مساحة التحديث." }
                val destination = File(directory, "$APK_FILE_PREFIX${validatedManifest.versionCode}.apk")

                stage = "cached_apk_validation"
                if (destination.isFile && isVerifiedCandidate(appContext, destination, validatedManifest)) return@withLock destination
                destination.delete()
                val temporary = File(directory, "${destination.name}.part")
                temporaryFile = temporary
                stage = "apk_download"
                downloadApk(validatedManifest, temporary, onProgress)
                stage = "sha256_validation"
                verifySha256(temporary, validatedManifest.sha256)
                stage = "package_signature_and_compatibility"
                val verified = UpdateApkInstaller.inspectAndValidate(appContext, temporary)
                require(verified.versionCode == validatedManifest.versionCode) { "رقم إصدار APK لا يطابق ملف التحديث." }
                stage = "verified_file_commit"
                require(temporary.renameTo(destination)) { "تعذّر حفظ ملف التحديث." }
                directory.listFiles()?.filter { it != destination && (it.name.endsWith(".apk") || it.name.endsWith(".part")) }?.forEach { it.delete() }
                destination
            } catch (error: Exception) {
                temporaryFile?.delete()
                android.util.Log.w("AppUpdate", "APK acquisition failed at $stage (${error.javaClass.simpleName})")
                if (error is CancellationException) throw error
                throw error
            }
        }
    }

    /** Returns the already-downloaded APK only when its manifest hash and package metadata still verify. */
    suspend fun cachedApkForInstall(context: Context, manifest: UpdateManifest): File? = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val validManifest = UpdateManifestValidator.validate(manifest)
            val appContext = context.applicationContext
            val candidate = File(File(appContext.cacheDir, "updates"), "$APK_FILE_PREFIX${validManifest.versionCode}.apk")
            if (isVerifiedCandidate(appContext, candidate, validManifest)) candidate else null
        }
    }

    private fun fetchManifest(): Pair<String, UpdateManifest> {
        require(UpdatePolicy.isTrustedManifestUrl(manifestUrl.toString())) { "رابط معلومات التحديث غير موثوق." }
        val request = Request.Builder().url(manifestUrl).header("Cache-Control", "no-cache").get().build()
        client.newCall(request).execute().use { response ->
            require(response.code == 200) { "تعذّر جلب معلومات التحديث (HTTP ${response.code})." }
            validateResponseOrigin(response, manifest = true)
            val contentType = response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
            require(UpdatePolicy.isJsonContentType(contentType)) { "استجابة خادم التحديث ليست JSON." }
            val body = response.body ?: error("ملف معلومات التحديث فارغ.")
            val length = body.contentLength()
            require(length == -1L || length in 1L..MAX_MANIFEST_BYTES) { "حجم ملف معلومات التحديث غير صالح." }
            val bytes = body.byteStream().use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_MANIFEST_BYTES) { "ملف معلومات التحديث أكبر من المسموح." }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            val raw = bytes.toString(Charsets.UTF_8)
            return raw to UpdateManifestValidator.parseAndValidate(raw)
        }
    }

    private suspend fun downloadApk(
        manifest: UpdateManifest,
        temporary: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ) {
        val request = Request.Builder().url(manifest.apkUrl).header("Cache-Control", "no-cache").get().build()
        client.newCall(request).execute().use { response ->
            require(response.code == 200) { "تعذّر تنزيل التحديث (HTTP ${response.code})." }
            validateResponseOrigin(response, manifest = false)
            val contentType = response.header("Content-Type")
            require(UpdatePolicy.isSupportedContentType(contentType)) { "نوع ملف التحديث غير مدعوم." }
            val body = response.body ?: error("ملف APK فارغ.")
            val contentLength = body.contentLength()
            require(contentLength == -1L || contentLength in 1L..MAX_APK_BYTES) { "حجم ملف التحديث غير صالح." }
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
                        onProgress(written, contentLength)
                    }
                    output.fd.sync()
                }
            }
            require(written > 0L && (contentLength < 0L || written == contentLength)) { "اكتمل تنزيل APK بصورة غير صحيحة." }
        }
    }

    private fun isVerifiedCandidate(context: Context, file: File, manifest: UpdateManifest): Boolean = runCatching {
        verifySha256(file, manifest.sha256)
        val apk = UpdateApkInstaller.inspectAndValidate(context, file)
        require(apk.versionCode == manifest.versionCode)
        apk.versionCode > installedVersionCode(context)
    }.getOrDefault(false)

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        require(actual == expected) { "فشل التحقق من سلامة APK (SHA-256)." }
    }

    private fun validateResponseOrigin(response: Response, manifest: Boolean) {
        val trusted = if (manifest) UpdatePolicy.isTrustedManifestUrl(response.request.url.toString())
        else UpdatePolicy.isTrustedApkUrl(response.request.url.toString())
        require(trusted) { "رابط تحديث غير موثوق." }
    }
}

internal object UpdateVersionPolicy {
    fun isNewer(candidateVersionCode: Long, installedVersionCode: Long): Boolean = candidateVersionCode > installedVersionCode
    fun isRequired(installedVersionCode: Long, minimumSupportedVersionCode: Long): Boolean = installedVersionCode < minimumSupportedVersionCode
}

internal object UpdatePolicy {
    fun isTrustedManifestUrl(value: String): Boolean = value == UpdateManifestValidator.MANIFEST_URL

    fun isTrustedApkUrl(value: String): Boolean = runCatching {
        val url = value.toHttpUrl()
        url.isHttps && url.host.equals("download.techda.dev", ignoreCase = true) &&
            url.port == 443 && url.encodedPath == "/SahatiSukari.apk" && url.query == null && url.fragment == null && url.username.isEmpty() && url.password.isEmpty()
    }.getOrDefault(false)

    fun isSha256(value: String): Boolean = value.matches(Regex("(?i)[a-f0-9]{64}"))

    fun isJsonContentType(value: String?): Boolean = value == "application/json" || value?.endsWith("+json") == true

    fun isSupportedContentType(value: String?): Boolean {
        val type = value?.substringBefore(';')?.trim()?.lowercase()
        return type == "application/vnd.android.package-archive" || type == "application/octet-stream"
    }
}

data class UpdateCheck(
    val installedVersionCode: Long,
    val manifest: UpdateManifest,
    val available: Boolean,
    val required: Boolean,
    val checkedAtMillis: Long = System.currentTimeMillis(),
) {
    val availableVersionCode: Long get() = manifest.versionCode
    val availableVersionName: String get() = manifest.versionName
}
