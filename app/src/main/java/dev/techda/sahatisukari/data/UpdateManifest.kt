package dev.techda.sahatisukari.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class UpdateManifest(
    val applicationId: String,
    val versionCode: Long,
    val versionName: String,
    val minimumSupportedVersionCode: Long,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String,
)

object UpdateStartupPolicy {
    fun shouldCheck(authIsChecking: Boolean, alreadyCheckedThisLaunch: Boolean): Boolean =
        !authIsChecking && !alreadyCheckedThisLaunch

    fun shouldShowPrompt(available: Boolean, dismissedVersionCode: Long?, availableVersionCode: Long): Boolean =
        available && dismissedVersionCode != availableVersionCode
}

internal object UpdateManifestValidator {
    const val MANIFEST_URL = "https://download.techda.dev/updates.json"
    const val APK_URL = "https://download.techda.dev/SahatiSukari.apk"
    const val APPLICATION_ID = "dev.techda.sahatisukari"

    private val json = Json { ignoreUnknownKeys = false }

    fun parseAndValidate(raw: String): UpdateManifest {
        require(raw.toByteArray(Charsets.UTF_8).size <= 64 * 1024) { "ملف معلومات التحديث أكبر من المسموح." }
        val manifest = try {
            json.decodeFromString<UpdateManifest>(raw)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("ملف معلومات التحديث غير صالح.", error)
        }
        return validate(manifest)
    }

    fun validate(manifest: UpdateManifest): UpdateManifest {
        require(manifest.applicationId == APPLICATION_ID) { "ملف التحديث لا يخص هذا التطبيق." }
        require(manifest.versionCode > 0L) { "رقم إصدار التحديث غير صالح." }
        require(manifest.versionName.isNotBlank() && manifest.versionName.length <= 40) { "اسم إصدار التحديث غير صالح." }
        require(manifest.minimumSupportedVersionCode in 1L..manifest.versionCode) { "الحد الأدنى للإصدار غير صالح." }
        require(UpdatePolicy.isTrustedApkUrl(manifest.apkUrl)) { "رابط APK غير موثوق." }
        require(UpdatePolicy.isSha256(manifest.sha256)) { "بصمة APK غير صالحة." }
        require(manifest.releaseNotes.length <= 1000) { "ملاحظات الإصدار طويلة أكثر من المسموح." }
        return manifest.copy(sha256 = manifest.sha256.lowercase())
    }
}
