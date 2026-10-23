package dev.techda.sahatisukari.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toPixelMap
import dev.techda.sahatisukari.ui.theme.SahatiTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream
import kotlin.math.abs

class UiCorrectionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun statCardArabicContentAlignsToPhysicalRightInRtlLayout() {
        composeRule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                SahatiTheme {
                    Box(Modifier.width(360.dp).wrapContentHeight()) {
                        StatCard("آخر قراءة سكر", "١٢٥", "لم تسجل أي قراءة اليوم", Icons.Outlined.Assessment)
                    }
                }
            }
        }

        val cardWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val label = composeRule.onNodeWithText("آخر قراءة سكر").fetchSemanticsNode().boundsInRoot
        assertTrue("Arabic stat label should sit on the physical right in RTL", label.center.x > cardWidth / 2f)
    }

    @Test fun dashboardSummaryRowsPlaceArabicLabelsRightOfReadableValuesInRtlLayout() {
        val rows = listOf(
            "الوجبات المسجّلة" to "0",
            "الأعراض المسجّلة" to "1",
            "الوزن الأخير" to "122.50 كغم",
            "آخر HbA1c مسجّل" to "5.8%",
        )
        val fontScale = mutableStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale.value),
            ) {
                SahatiTheme {
                    Card(
                        Modifier.width(360.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ملخص اليوم", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            rows.forEach { (label, value) -> SummaryRow(label, value) }
                        }
                    }
                }
            }
        }

        var standardLabelHeight = 0f
        listOf(1f, 1.3f).forEachIndexed { index, scale ->
            if (index > 0) {
                composeRule.runOnIdle { fontScale.value = scale }
                composeRule.waitForIdle()
            }
            rows.forEach { (labelText, valueText) ->
                val labelNode = composeRule.onNodeWithText(labelText).assertIsDisplayed().fetchSemanticsNode()
                val valueNode = composeRule.onNodeWithText(valueText).assertIsDisplayed().fetchSemanticsNode()
                val label = labelNode.boundsInRoot
                val value = valueNode.boundsInRoot
                assertTrue("$labelText should be physically right of $valueText", label.center.x > value.center.x)
                assertTrue("$labelText and $valueText must not overlap at font scale $scale", value.right <= label.left)
                assertTrue("$labelText must have visible width", label.width > 0f)
                assertTrue("$valueText must have visible width", value.width > 0f)
                if (index == 0 && labelText == rows.first().first) standardLabelHeight = label.height
                if (index == 1 && labelText == rows.first().first) {
                    assertTrue("Text should scale at 1.3x font scale", label.height > standardLabelHeight)
                }
            }

            composeRule.waitForIdle()
            val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
            val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
            val outputUri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "dashboard-summary-rtl-${scale}x.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                },
            ) ?: error("Could not create emulator screenshot")
            resolver.openOutputStream(outputUri).use { output: OutputStream? ->
                checkNotNull(output).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    @Test fun pageTitleAlignsToRightInRtlLayout() {
        composeRule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                SahatiTheme { SahatiPageTitle("عنوان الملف") }
            }
        }

        val pixels = composeRule.onNodeWithTag("sahati-page-title").captureToImage().toPixelMap()
        var leftInk = 0
        var rightInk = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (pixel.alpha > 0.5f && pixel.red < 0.6f && pixel.green < 0.6f && pixel.blue < 0.6f) {
                    if (x < pixels.width / 2) leftInk++ else rightInk++
                }
            }
        }
        assertTrue("Arabic title should be aligned to the physical right in RTL", rightInk > leftInk * 2)
    }

    @Test fun sixNavigationItemsUseEqualWidthAndCenteredLabels() {
        val selected = mutableIntStateOf(0)
        composeRule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                SahatiTheme {
                    Box(Modifier.width(360.dp).wrapContentHeight()) {
                        SahatiBottomNavigation(selected.intValue, { selected.intValue = it })
                    }
                }
            }
        }

        assertEquals(6, sahatiDestinations.size)
        val firstItemWidth = composeRule.onNodeWithTag("sahati-nav-item-0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        sahatiDestinations.forEachIndexed { index, destination ->
            composeRule.onNodeWithText(destination.title, useUnmergedTree = true).assertIsDisplayed()
            val icon = composeRule.onNodeWithTag("sahati-nav-icon-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val label = composeRule.onNodeWithTag("sahati-nav-label-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val item = composeRule.onNodeWithTag("sahati-nav-item-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("${destination.title} center alignment", abs(icon.center.x - label.center.x) <= 1f)
            assertTrue("${destination.title} equal width", abs(item.width - firstItemWidth) <= 1f)
        }

        composeRule.onNodeWithText("قياسات السكر", useUnmergedTree = true).performClick()
        composeRule.runOnIdle { assertEquals(2, selected.intValue) }
    }

    @Test fun navigationLabelsRemainVisibleWithIncreasedFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density = 1f, fontScale = 1.35f),
            ) {
                SahatiTheme {
                    Box(Modifier.width(360.dp).wrapContentHeight()) {
                        SahatiBottomNavigation(0, {})
                    }
                }
            }
        }
        sahatiDestinations.indices.forEach { index ->
            composeRule.onNodeWithTag("sahati-nav-label-$index", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test fun healthProfileShowsArabicValuesAndHidesRawApiFields() {
        val model = healthProfilePresentation(profileFixture())
        composeRule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                SahatiTheme { HealthProfileContent(model, {}) }
            }
        }
        composeRule.onNodeWithText("ارتفاع ضغط الدم").assertIsDisplayed()
        composeRule.onNodeWithText("غير مُشخّصة").assertIsDisplayed()
        composeRule.onNodeWithText("ملغم/دل").assertExists()
        composeRule.onNodeWithText("patientId").assertDoesNotExist()
        composeRule.onNodeWithText("conditionType").assertDoesNotExist()
        composeRule.onNodeWithText("NOT_DIAGNOSED").assertDoesNotExist()
        composeRule.onNodeWithText("synthetic-profile-uuid").assertDoesNotExist()
    }

    @Test fun conditionEditorSubmitsAllConditionsAndPreservesNotes() {
        val saved = mutableStateOf<Pair<String, JsonObject>?>(null)
        composeRule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                SahatiTheme {
                    Column(Modifier.height(700.dp).verticalScroll(rememberScrollState())) {
                        ProfileEditor(profileFixture(), false) { route, body -> saved.value = route to body }
                    }
                }
            }
        }
        composeRule.onNodeWithTag("profile-section-conditions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ارتفاع ضغط الدم — الحالة: غير مُشخّصة").assertExists()
        composeRule.onNodeWithTag("profile-save").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals("profile:conditions", saved.value?.first)
            val conditions = saved.value?.second?.get("conditions")?.jsonArray.orEmpty().map { it.jsonObject }
            assertEquals(8, conditions.size)
            val hypertension = conditions.first { it["conditionType"]?.jsonPrimitive?.content == "HYPERTENSION" }
            assertEquals("NOT_DIAGNOSED", hypertension["status"]?.jsonPrimitive?.content)
            assertEquals("ملاحظة تجريبية", hypertension["notes"]?.jsonPrimitive?.content)
        }
    }

    private fun profileFixture() = Json.parseToJsonElement(
        """{
          "id":"synthetic-profile-uuid",
          "patientId":"synthetic-patient-uuid",
          "fullName":"مريضة تجريبية",
          "gender":"FEMALE",
          "dateOfBirth":"1990-01-02",
          "age":36,
          "heightCm":165,
          "nationalIdMasked":"••••1234",
          "email":"patient@example.test",
          "phoneNumber":"+9647000000000",
          "preferredGlucoseUnit":"MG_DL",
          "numeralSystem":"LATIN",
          "completion":{"percent":70,"missing":[]},
          "diabetes":{"diabetesType":"TYPE_2"},
          "conditions":[{"id":"synthetic-condition-uuid","patientId":"synthetic-patient-uuid","conditionType":"HYPERTENSION","status":"NOT_DIAGNOSED","notes":"ملاحظة تجريبية"}],
          "reproductive":null,
          "allergies":null,
          "smokingStatus":"NEVER",
          "familyHistoryDiabetes":"UNSURE",
          "healthNotes":null
        }""".trimIndent(),
    ).jsonObject
}
