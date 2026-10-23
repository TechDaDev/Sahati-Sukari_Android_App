package dev.techda.sahatisukari.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun onlyGreaterVersionCodeIsAnUpdate() {
        assertTrue(UpdateVersionPolicy.isNewer(11, 10))
        assertFalse(UpdateVersionPolicy.isNewer(10, 10))
        assertFalse(UpdateVersionPolicy.isNewer(9, 10))
    }

    @Test
    fun requiredUpdateUsesMinimumSupportedVersion() {
        assertTrue(UpdateVersionPolicy.isRequired(3, 4))
        assertFalse(UpdateVersionPolicy.isRequired(4, 4))
        assertFalse(UpdateVersionPolicy.isRequired(5, 4))
    }

    @Test
    fun foregroundCheckRunsOnceAfterAuthenticationStateIsReady() {
        assertFalse(UpdateStartupPolicy.shouldCheck(authIsChecking = true, alreadyCheckedThisLaunch = false))
        assertTrue(UpdateStartupPolicy.shouldCheck(authIsChecking = false, alreadyCheckedThisLaunch = false))
        assertFalse(UpdateStartupPolicy.shouldCheck(authIsChecking = false, alreadyCheckedThisLaunch = true))
    }

    @Test
    fun laterDismissalSuppressesOnlyThatVersionForTheCurrentSession() {
        assertTrue(UpdateStartupPolicy.shouldShowPrompt(available = true, dismissedVersionCode = null, availableVersionCode = 9))
        assertFalse(UpdateStartupPolicy.shouldShowPrompt(available = true, dismissedVersionCode = 9, availableVersionCode = 9))
        assertTrue(UpdateStartupPolicy.shouldShowPrompt(available = true, dismissedVersionCode = 9, availableVersionCode = 10))
        assertFalse(UpdateStartupPolicy.shouldShowPrompt(available = false, dismissedVersionCode = null, availableVersionCode = 9))
    }

    @Test
    fun onlyTheExactHttpsApkUrlIsTrusted() {
        assertTrue(UpdatePolicy.isTrustedApkUrl("https://download.techda.dev/SahatiSukari.apk"))
        assertFalse(UpdatePolicy.isTrustedApkUrl("http://download.techda.dev/SahatiSukari.apk"))
        assertFalse(UpdatePolicy.isTrustedApkUrl("https://download.techda.dev.evil.test/SahatiSukari.apk"))
        assertFalse(UpdatePolicy.isTrustedApkUrl("https://download.techda.dev/other.apk"))
        assertFalse(UpdatePolicy.isTrustedApkUrl("https://download.techda.dev/SahatiSukari.apk?redirect=https://evil.test"))
    }

    @Test
    fun acceptsOnlyApkOrBinaryContentTypes() {
        assertTrue(UpdatePolicy.isSupportedContentType("application/vnd.android.package-archive"))
        assertTrue(UpdatePolicy.isSupportedContentType("application/octet-stream; charset=binary"))
        assertFalse(UpdatePolicy.isSupportedContentType("text/html"))
        assertFalse(UpdatePolicy.isSupportedContentType(null))
    }

    @Test
    fun acceptsValidTrustedManifest() {
        val manifest = UpdateManifestValidator.parseAndValidate(validManifest())
        assertEquals("dev.techda.sahatisukari", manifest.applicationId)
        assertEquals(7L, manifest.versionCode)
        assertEquals("0.2.5", manifest.versionName)
    }

    @Test
    fun rejectsUntrustedOrMalformedManifestFields() {
        val base = validManifest()
        assertInvalid(base.replace("dev.techda.sahatisukari", "other.package"))
        assertInvalid(base.replace("https://download.techda.dev/SahatiSukari.apk", "http://download.techda.dev/SahatiSukari.apk"))
        assertInvalid(base.replace("a".repeat(64), "bad-hash"))
        assertInvalid(base.replace("\"minimumSupportedVersionCode\":1", "\"minimumSupportedVersionCode\":8"))
        assertInvalid("not json")
    }

    @Test
    fun updateManifestUrlsAndJsonContentTypesAreStrict() {
        assertTrue(UpdatePolicy.isTrustedManifestUrl("https://download.techda.dev/updates.json"))
        assertFalse(UpdatePolicy.isTrustedManifestUrl("http://download.techda.dev/updates.json"))
        assertTrue(UpdatePolicy.isJsonContentType("application/json"))
        assertTrue(UpdatePolicy.isJsonContentType("application/problem+json"))
        assertFalse(UpdatePolicy.isJsonContentType("text/html"))
    }

    private fun assertInvalid(json: String) {
        try {
            UpdateManifestValidator.parseAndValidate(json)
            throw AssertionError("Expected invalid update manifest to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected for invalid field values and invalid JSON.
        }
    }

    private fun validManifest() = """
        {
          "applicationId":"dev.techda.sahatisukari",
          "versionCode":7,
          "versionName":"0.2.5",
          "minimumSupportedVersionCode":1,
          "apkUrl":"https://download.techda.dev/SahatiSukari.apk",
          "sha256":"${"a".repeat(64)}",
          "releaseNotes":"تحسينات وإصلاحات"
        }
    """.trimIndent()
}
