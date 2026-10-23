package dev.techda.sahatisukari.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun onlyGreaterVersionCodeIsAnUpdate() {
        assertTrue(UpdateVersionPolicy.isNewer(11, 10))
        assertFalse(UpdateVersionPolicy.isNewer(10, 10))
        assertFalse(UpdateVersionPolicy.isNewer(9, 10))
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
}
