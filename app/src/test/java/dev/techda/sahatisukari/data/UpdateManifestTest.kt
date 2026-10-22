package dev.techda.sahatisukari.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestTest {
    private val valid = """{"applicationId":"dev.techda.sahatisukari","versionCode":3,"versionName":"0.3.0","minimumSupportedVersionCode":1,"apkUrl":"https://download.techda.dev/SahatiSukari.apk","sha256":"${"a".repeat(64)}","releaseNotes":"تحسينات"}"""

    @Test fun acceptsTrustedManifestAndComparesVersionCodes() {
        val manifest = UpdateManifestValidator.parseAndValidate(valid)
        assertEquals(3, manifest.versionCode)
        assertFalse(UpdateCheck(manifest, manifest.versionCode > 3).available)
        assertFalse(UpdateCheck(manifest, manifest.versionCode > 4).available)
        assertTrue(UpdateCheck(manifest, manifest.versionCode > 2).available)
    }

    @Test fun rejectsWrongApplicationId() {
        rejected(valid.replace("dev.techda.sahatisukari", "other.app"))
    }

    @Test fun rejectsUntrustedHostsAndHttp() {
        rejected(valid.replace("https://download.techda.dev/", "https://evil.example/"))
        rejected(valid.replace("https://download.techda.dev/", "http://download.techda.dev/"))
    }

    @Test fun rejectsBadChecksumAndMissingFields() {
        rejected(valid.replace("a".repeat(64), "xyz"))
        rejected(valid.replace("\"versionName\":\"0.3.0\",", ""))
    }

    private fun rejected(body: String) {
        try {
            UpdateManifestValidator.parseAndValidate(body)
            throw AssertionError("Invalid manifest was accepted")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        } catch (_: kotlinx.serialization.SerializationException) {
            // Expected parse failure.
        }
    }
}
