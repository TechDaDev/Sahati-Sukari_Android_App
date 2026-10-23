package dev.techda.sahatisukari.data

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateInstallerIntentTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun installerIntentUsesContentUriAndTemporaryReadGrant() {
        val apkUri = Uri.parse("content://${context.packageName}.fileprovider/updates/test.apk")
        val intent = UpdateApkInstaller.packageInstallerIntent(apkUri)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data?.scheme)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNotNull(intent.clipData)
        assertEquals(apkUri, intent.clipData?.getItemAt(0)?.uri)
    }

    @Test
    fun installPermissionSettingsAreScopedToThisApplication() {
        val intent = UpdateApkInstaller.unknownSourcesSettingsIntent(context)

        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:${context.packageName}", intent.data?.toString())
    }

    @Test
    fun invalidApkIsRejectedBeforeInstallRequest() {
        val invalidApk = java.io.File(context.cacheDir, "invalid-update-test.apk")
        invalidApk.writeText("not an apk")
        try {
            try {
                UpdateApkInstaller.inspectAndValidate(context, invalidApk)
                throw AssertionError("Invalid APK was accepted")
            } catch (_: IllegalArgumentException) {
                // Expected: the APK archive has no package metadata.
            } catch (_: IllegalStateException) {
                // Expected: package manager rejects this malformed archive.
            }
        } finally {
            invalidApk.delete()
        }
    }
}
