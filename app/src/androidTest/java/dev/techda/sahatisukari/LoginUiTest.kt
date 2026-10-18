package dev.techda.sahatisukari

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun loginScreenShowsArabicRtlEntryAndEmergencyAction() {
        composeRule.onNodeWithText("صحتي وسكري").assertIsDisplayed()
        composeRule.onNodeWithText("تسجيل الدخول").assertIsDisplayed()
        composeRule.onNodeWithText("مساعدة عاجلة — 911").assertIsDisplayed()
        composeRule.onNodeWithText("مساعدة عاجلة — 911").performClick()
        composeRule.onNodeWithText("رقم الطوارئ الموحد في العراق").assertIsDisplayed()
        composeRule.onNodeWithText("911").assertIsDisplayed()
        composeRule.onNodeWithText("العودة إلى تسجيل الدخول").performClick()
        composeRule.onNodeWithText("تسجيل الدخول").assertIsDisplayed()
    }
}
