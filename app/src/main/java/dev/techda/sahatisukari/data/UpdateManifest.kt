package dev.techda.sahatisukari.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateManifest(
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
    val minimumSupportedVersionCode: Int = 1,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String? = null,
)

object UpdateManifestValidator {
    const val APPLICATION_ID = "dev.techda.sahatisukari"
    const val MANIFEST_URL = "https://download.techda.dev/updates.json"
    const val APK_URL = "https://download.techda.dev/SahatiSukari.apk"
    private val json = Json { ignoreUnknownKeys = false }

    fun parseAndValidate(body: String): UpdateManifest {
        val manifest = json.decodeFromString<UpdateManifest>(body)
        require(manifest.applicationId == APPLICATION_ID) { "تطبيق غير متوقع." }
        require(manifest.versionCode > 0 && manifest.minimumSupportedVersionCode > 0) { "رقم إصدار غير صالح." }
        require(manifest.versionName.isNotBlank()) { "اسم إصدار مفقود." }
        val uri = java.net.URI(manifest.apkUrl)
        require(uri.scheme == "https" && uri.host.equals("download.techda.dev", ignoreCase = true) && uri.userInfo == null && uri.port in listOf(-1, 443)) { "رابط تحديث غير موثوق." }
        require(uri.path == "/SahatiSukari.apk" && uri.query == null && uri.fragment == null) { "مسار تحديث غير متوقع." }
        require(manifest.sha256.matches(Regex("(?i)[a-f0-9]{64}"))) { "بصمة تحديث غير صالحة." }
        require(manifest.minimumSupportedVersionCode <= manifest.versionCode) { "حد أدنى للإصدار غير صالح." }
        return manifest
    }
}
