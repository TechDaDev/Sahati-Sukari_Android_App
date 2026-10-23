package dev.techda.sahatisukari.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ActiveSessionCard(
    /** Internal API key used only for the authenticated revoke call; never render this value. */
    val id: String,
    val deviceName: String,
    val description: String,
    val createdAt: String,
    val lastActivity: String,
)

internal fun activeSessionCards(data: JsonObject?, timeZoneId: String?, numeralSystem: String?): List<ActiveSessionCard> {
    val rows = (data?.get("sessions") ?: data?.get("items")) as? JsonArray ?: return emptyList()
    return rows.mapNotNull { element ->
        val record = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val id = record.sessionValue("id") ?: return@mapNotNull null
        val rawLabel = record.sessionValue("deviceLabel")
            ?.filterNot(Char::isISOControl)
            ?.take(120)
            ?.takeIf(String::isNotBlank)
        val deviceName = when {
            rawLabel == null -> "جهاز غير معروف"
            rawLabel.equals("Android", ignoreCase = true) -> "أندرويد"
            else -> rawLabel
        }
        ActiveSessionCard(
            id = id,
            deviceName = deviceName,
            description = "جلسة تسجيل دخول",
            createdAt = MedicalPresentation.formatInstant(record.sessionValue("createdAt"), timeZoneId, numeralSystem),
            lastActivity = MedicalPresentation.formatInstant(record.sessionValue("lastUsedAt"), timeZoneId, numeralSystem),
        )
    }
}

internal data class SessionConfirmationState(
    val selectedSession: ActiveSessionCard? = null,
    val revokeAllPending: Boolean = false,
) {
    fun askToRevoke(session: ActiveSessionCard) = copy(selectedSession = session, revokeAllPending = false)
    fun askToRevokeAll() = copy(selectedSession = null, revokeAllPending = true)
    fun cancel() = SessionConfirmationState()
    fun confirmedSessionId(): String? = selectedSession?.id
    fun confirmedRevokeAll(): Boolean = revokeAllPending
}

internal fun SettingsState.canStartSettingsRequest(): Boolean = !busy && !signedOut

internal fun SettingsState.afterSettingsFailure(status: Int?, message: String): SettingsState =
    if (status == 401 || status == 403) {
        copy(busy = false, signedOut = true, error = null, notice = "انتهت الجلسة. سجّل الدخول مجددًا.")
    } else {
        copy(busy = false, error = message)
    }

private fun JsonObject.sessionValue(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull?.trim() }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true) }
