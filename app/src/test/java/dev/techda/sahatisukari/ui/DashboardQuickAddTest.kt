package dev.techda.sahatisukari.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardQuickAddTest {
    @Test
    fun allActionsOpenTheirCreateDestinationAndNeverCarryAnEditingRecord() {
        val expected = listOf(
            Triple(DashboardQuickAdd.Glucose, 2, "glucose"),
            Triple(DashboardQuickAdd.Medication, 3, "administrations"),
            Triple(DashboardQuickAdd.Meal, 4, "meals"),
            Triple(DashboardQuickAdd.Activity, 4, "activities"),
        )

        expected.forEachIndexed { index, (action, section, endpoint) ->
            val flow = expected.take(index + 1).fold(DashboardQuickAddFlow()) { current, item -> current.open(item.first) }
            assertEquals(section, flow.section)
            assertEquals(endpoint, action.endpoint)
            assertEquals(action, flow.request?.action)
            assertEquals(index + 1, flow.request?.requestId)
            assertNull("Quick Add must not target an existing record", flow.request?.recordId)
            assertEquals(
                when (action) {
                    DashboardQuickAdd.Glucose -> DashboardQuickAddForm.MedicalRecord
                    DashboardQuickAdd.Medication -> DashboardQuickAddForm.MedicationAdministration
                    else -> DashboardQuickAddForm.JournalRecord
                },
                action.form,
            )
        }
    }

    @Test
    fun rowsKeepTheSpecifiedRtlActionOrder() {
        assertEquals(
            listOf(
                listOf(DashboardQuickAdd.Glucose, DashboardQuickAdd.Medication),
                listOf(DashboardQuickAdd.Meal, DashboardQuickAdd.Activity),
            ),
            dashboardQuickAddRows,
        )
    }

    @Test
    fun cancelReturnsToDashboardWithoutNoticeOrRefresh() {
        val form = DashboardQuickAddFlow().open(DashboardQuickAdd.Meal)
        val cancelled = form.cancel()

        assertEquals(0, cancelled.section)
        assertNull(cancelled.request)
        assertNull(cancelled.notice)
        assertEquals(0, cancelled.dashboardRefreshCount)
    }

    @Test
    fun saveReturnsToDashboardWithLocalizedNoticeAndOneRefresh() {
        DashboardQuickAdd.entries.forEach { action ->
            val saved = DashboardQuickAddFlow().open(action).completeSave(true)

            assertEquals(0, saved.section)
            assertNull(saved.request)
            assertEquals(1, saved.dashboardRefreshCount)
            assertEquals(action.successMessage, saved.notice)
        }
    }

    @Test
    fun failedSaveKeepsTheCreateDestinationAndDoesNotRefresh() {
        val form = DashboardQuickAddFlow().open(DashboardQuickAdd.Activity)
        val failed = form.completeSave(false)

        assertEquals(form, failed)
        assertEquals(4, failed.section)
        assertEquals("activities", failed.journalEndpoint)
        assertEquals(0, failed.dashboardRefreshCount)
        assertNull(failed.notice)
    }

    @Test
    fun selectingAnotherBottomSectionClearsPendingQuickAddRoute() {
        val selected = DashboardQuickAddFlow().open(DashboardQuickAdd.Medication).selectSection(1)

        assertEquals(1, selected.section)
        assertNull(selected.request)
    }

    @Test
    fun reopeningSameActionGetsAFreshCreateRequest() {
        val first = DashboardQuickAddFlow().open(DashboardQuickAdd.Meal)
        val reopened = first.cancel().open(DashboardQuickAdd.Meal)

        assertEquals(1, first.request?.requestId)
        assertEquals(2, reopened.request?.requestId)
        assertNull(reopened.request?.recordId)
    }

    @Test
    fun saveGuardRejectsDuplicateSubmissionsAndAllowsRetryAfterCompletion() {
        val guard = DuplicateSubmissionGuard()

        assertTrue(guard.tryStart())
        assertFalse(guard.tryStart())
        guard.finish()
        assertTrue(guard.tryStart())
    }
}
