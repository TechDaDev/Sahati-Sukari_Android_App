package dev.techda.sahatisukari.data

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object UpdateApkInstaller {
    fun inspectAndValidate(context: Context, apk: File): VerifiedApk {
        var stage = "apk_file"
        try {
            require(apk.isFile && apk.length() > 0L) { "ملف التحديث غير موجود." }
            stage = "apk_structure"
            val packageManager = context.packageManager
            val flags = signingFlags()
            val downloaded = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
                ?: error("ملف APK غير صالح.")
            stage = "application_id"
            require(downloaded.packageName == context.packageName) { "ملف التحديث لا يخص هذا التطبيق." }
            stage = "release_build"
            require(downloaded.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                "ملف التحديث ليس إصدارًا إنتاجيًا."
            }
            stage = "device_compatibility"
            val minSdk = downloaded.applicationInfo?.minSdkVersion ?: 1
            require(minSdk <= Build.VERSION.SDK_INT) { "هذا التحديث لا يدعم إصدار Android على جهازك." }
            stage = "signing_identity"
            val installed = packageManager.getPackageInfo(context.packageName, flags)
            val candidateSigners = signatures(downloaded)
            require(candidateSigners.isNotEmpty() && candidateSigners == signatures(installed)) {
                "توقيع APK لا يطابق توقيع التطبيق المثبت."
            }
            return VerifiedApk(downloaded.versionCodeLongCompat(), downloaded.versionName.orEmpty())
        } catch (error: Exception) {
            android.util.Log.w("AppUpdate", "APK validation failed at $stage (${error.javaClass.simpleName})")
            throw error
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun createInstallerIntent(context: Context, apk: File, installedVersionCode: Long): Intent {
        var stage = "apk_validation"
        try {
            val verified = inspectAndValidate(context, apk)
            stage = "version_check"
            require(verified.versionCode > installedVersionCode) { "لا يحتوي ملف APK على إصدار أحدث." }
            stage = "install_permission"
            check(canRequestPackageInstalls(context)) { "يتطلب Android السماح للتطبيق بتثبيت التحديثات." }
            stage = "file_provider_uri"
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            stage = "installer_intent"
            val intent = packageInstallerIntent(apkUri)
            check(intent.resolveActivity(context.packageManager) != null) { "لا يتوفر مثبت حزم على هذا الجهاز." }
            android.util.Log.i("AppUpdate", "Installer request ready; stage=$stage")
            return intent
        } catch (error: Exception) {
            android.util.Log.w("AppUpdate", "Installer request failed at $stage (${error.javaClass.simpleName})")
            throw error
        }
    }

    fun launchInstaller(activity: Activity, apk: File, installedVersionCode: Long) {
        check(!activity.isFinishing && !activity.isDestroyed) { "تعذّر فتح مثبت Android من هذه الشاشة." }
        activity.startActivity(createInstallerIntent(activity, apk, installedVersionCode))
    }

    fun packageInstallerIntent(apkUri: Uri): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("SahatiSukari update", apkUri)
        return intent
    }

    private fun PackageInfo.versionCodeLongCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else @Suppress("DEPRECATION") versionCode.toLong()

    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
    }

    private fun signatures(info: PackageInfo): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        } else {
            @Suppress("DEPRECATION") info.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
        }
}

data class VerifiedApk(val versionCode: Long, val versionName: String)
