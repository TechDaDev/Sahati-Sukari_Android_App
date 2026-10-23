package dev.techda.sahatisukari.data

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
        require(apk.isFile && apk.length() > 0L) { "ملف التحديث غير موجود." }
        val packageManager = context.packageManager
        val flags = signingFlags()
        val downloaded = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("ملف APK غير صالح.")
        require(downloaded.packageName == context.packageName) { "ملف التحديث لا يخص هذا التطبيق." }
        require(downloaded.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            "ملف التحديث ليس إصدارًا إنتاجيًا."
        }
        val minSdk = downloaded.applicationInfo?.minSdkVersion ?: 1
        require(minSdk <= Build.VERSION.SDK_INT) { "هذا التحديث لا يدعم إصدار Android على جهازك." }
        val installed = packageManager.getPackageInfo(context.packageName, flags)
        val candidateSigners = signatures(downloaded)
        require(candidateSigners.isNotEmpty() && candidateSigners == signatures(installed)) {
            "توقيع APK لا يطابق توقيع التطبيق المثبت."
        }
        return VerifiedApk(downloaded.versionCodeLongCompat(), downloaded.versionName.orEmpty())
    }

    fun requestInstall(context: Context, apk: File, installedVersionCode: Int): Boolean {
        val verified = inspectAndValidate(context, apk)
        require(verified.versionCode > installedVersionCode.toLong()) { "لا يحتوي ملف APK على إصدار أحدث." }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        return true
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
