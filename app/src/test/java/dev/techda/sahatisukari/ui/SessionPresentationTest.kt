package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPresentationTest {
    @Test
    fun cardUsesOnlyAvailableMetadataAndFormatsCreationAndActivitySeparately() {
        val data = Json.parseToJsonElement(
            """{"sessions":[{"id":"synthetic-session-1","deviceLabel":"Android","createdAt":"2026-10-20T07:00:00Z","lastUsedAt":"2026-10-23T10:34:36.345Z","refreshExpiresAt":"2026-11-19T07:00:00Z"}]}""",
        ).jsonObject

        val card = activeSessionCards(data, "Asia/Baghdad", "LATIN").single()

        assertEquals("أندرويد", card.deviceName)
        assertEquals("جلسة تسجيل دخول", card.description)
        assertEquals("20/10/2026 – 10:00 صباحًا", card.createdAt)
        assertEquals("23/10/2026 – 1:34 مساءً", card.lastActivity)
        assertFalse(card.toString().contains("هذا الجهاز"))
        assertFalse(card.toString().contains("2026-10-23T"))
        assertFalse(card.toString().contains("refreshExpiresAt"))
    }

    @Test
    fun nullMissingAndSentinelValuesUseSafeArabicFallbacks() {
        val data = Json.parseToJsonElement(
            """{"sessions":[{"id":"synthetic-session-2","deviceLabel":null,"createdAt":null,"lastUsedAt":"undefined"}]}""",
        ).jsonObject

        val card = activeSessionCards(data, null, null).single()

        assertEquals("جهاز غير معروف", card.deviceName)
        assertEquals("جلسة تسجيل دخول", card.description)
        assertEquals(MedicalPresentation.UNAVAILABLE_DATE, card.createdAt)
        assertEquals(MedicalPresentation.UNAVAILABLE_DATE, card.lastActivity)
        assertFalse(card.toString().contains("null"))
        assertFalse(card.toString().contains("undefined"))
    }

    @Test
    fun unsupportedTimezoneFallsBackToBaghdadAndInvalidDateIsGraceful() {
        val data = Json.parseToJsonElement(
            """{"sessions":[{"id":"synthetic-session-3","createdAt":"2026-10-23T10:34:36.345Z","lastUsedAt":"bad-date"}]}""",
        ).jsonObject

        val card = activeSessionCards(data, "Unsupported/Zone", "LATIN").single()

        assertEquals("23/10/2026 – 1:34 مساءً", card.createdAt)
        assertEquals(MedicalPresentation.UNAVAILABLE_DATE, card.lastActivity)
    }

    @Test
    fun emptyAndMalformedListsDoNotInventSessions() {
        assertTrue(activeSessionCards(null, null, null).isEmpty())
        assertTrue(activeSessionCards(Json.parseToJsonElement("""{"sessions":[]}""").jsonObject, null, null).isEmpty())
        assertTrue(activeSessionCards(Json.parseToJsonElement("""{"sessions":[{"deviceLabel":"Android"}]}""").jsonObject, null, null).isEmpty())
    }

    @Test
    fun revokeConfirmationTargetsTheChosenSessionAndCancelProducesNoAction() {
        val cards = activeSessionCards(
            Json.parseToJsonElement("""{"sessions":[{"id":"first"},{"id":"second"}]}""").jsonObject,
            null,
            null,
        )
        val confirmation = SessionConfirmationState().askToRevoke(cards[1])

        assertEquals("second", confirmation.confirmedSessionId())
        assertFalse(confirmation.confirmedRevokeAll())
        assertNull(confirmation.cancel().confirmedSessionId())
    }

    @Test
    fun revokeAllConfirmationIsExplicitAndSettingsActionGatePreventsDuplicates() {
        val confirmation = SessionConfirmationState().askToRevokeAll()

        assertTrue(confirmation.confirmedRevokeAll())
        assertFalse(confirmation.cancel().confirmedRevokeAll())
        assertTrue(SettingsState().canStartSettingsRequest())
        assertFalse(SettingsState(busy = true).canStartSettingsRequest())
        assertFalse(SettingsState(signedOut = true).canStartSettingsRequest())
    }

    @Test
    fun authenticationFailureSignsOutButServerFailurePreservesSessionState() {
        val prior = SettingsState(
            sessions = Json.parseToJsonElement("""{"sessions":[{"id":"synthetic-session-4"}]}""").jsonObject,
            busy = true,
        )

        val unauthorized = prior.afterSettingsFailure(401, "error")
        assertFalse(unauthorized.busy)
        assertTrue(unauthorized.signedOut)
        assertNull(unauthorized.error)
        assertEquals("انتهت الجلسة. سجّل الدخول مجددًا.", unauthorized.notice)

        val serverFailure = prior.afterSettingsFailure(500, "تعذر الاتصال")
        assertFalse(serverFailure.busy)
        assertFalse(serverFailure.signedOut)
        assertEquals(prior.sessions, serverFailure.sessions)
        assertEquals("تعذر الاتصال", serverFailure.error)
    }
}
