package dev.techda.sahatisukari.ui

enum class DashboardQuickAddForm { MedicalRecord, MedicationAdministration, JournalRecord }

/** Destinations for the Dashboard's create-only shortcuts. */
enum class DashboardQuickAdd(val section: Int, val endpoint: String, val form: DashboardQuickAddForm, val successMessage: String) {
    Glucose(2, "glucose", DashboardQuickAddForm.MedicalRecord, "تم حفظ قياس السكر بنجاح"),
    Medication(3, "administrations", DashboardQuickAddForm.MedicationAdministration, "تم تسجيل الجرعة بنجاح"),
    Meal(4, "meals", DashboardQuickAddForm.JournalRecord, "تم حفظ الوجبة بنجاح"),
    Activity(4, "activities", DashboardQuickAddForm.JournalRecord, "تم حفظ النشاط بنجاح"),
}
internal val dashboardQuickAddRows = listOf(
    listOf(DashboardQuickAdd.Glucose, DashboardQuickAdd.Medication),
    listOf(DashboardQuickAdd.Meal, DashboardQuickAdd.Activity),
)

data class DashboardQuickAddRequest(val action: DashboardQuickAdd, val requestId: Int, val recordId: String? = null)

/** Small pure navigation model so quick-add routes can be verified without an emulator. */
data class DashboardQuickAddFlow(
    val section: Int = 0,
    val journalEndpoint: String = "meals",
    val request: DashboardQuickAddRequest? = null,
    val dashboardRefreshCount: Int = 0,
    val notice: String? = null,
    val nextRequestId: Int = 0,
) {
    fun open(action: DashboardQuickAdd) = copy(
        section = action.section,
        journalEndpoint = action.endpoint.takeIf { action.section == 4 } ?: "meals",
        request = DashboardQuickAddRequest(action, nextRequestId + 1),
        notice = null,
        nextRequestId = nextRequestId + 1,
    )

    fun cancel() = copy(section = 0, journalEndpoint = "meals", request = null, notice = null)

    fun completeSave(succeeded: Boolean) = if (!succeeded) failed() else copy(
        section = 0,
        journalEndpoint = "meals",
        request = null,
        dashboardRefreshCount = dashboardRefreshCount + 1,
        notice = request?.action?.successMessage,
    )

    fun selectSection(target: Int) = copy(
        section = target,
        journalEndpoint = if (target == 4) "meals" else journalEndpoint,
        request = null,
        notice = null,
    )

    /** Failed submissions keep the current form destination and do not request a dashboard refresh. */
    fun failed() = this
}

internal class DuplicateSubmissionGuard {
    private var inProgress = false

    @Synchronized
    fun tryStart(): Boolean {
        if (inProgress) return false
        inProgress = true
        return true
    }

    @Synchronized
    fun finish() {
        inProgress = false
    }
}
