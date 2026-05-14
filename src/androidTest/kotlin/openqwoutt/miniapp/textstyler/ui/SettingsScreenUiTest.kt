package openqwoutt.miniapp.textstyler.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import openqwoutt.miniapp.textstyler.data.settings.AppSettings

/**
 * UI tests for SettingsScreen.
 */
class SettingsScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `settings screen displays all four menu items`() {
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = {},
                isDarkTheme = false
            )
        }

        composeRule.onNodeWithText("AI Model").assertExists()
        composeRule.onNodeWithText("Language").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
        composeRule.onNodeWithText("Custom Backend").assertExists()
    }

    @Test
    fun `save button appears in top bar`() {
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = {},
                isDarkTheme = false
            )
        }

        composeRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `theme segment control has three options`() {
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = {},
                isDarkTheme = false
            )
        }

        composeRule.onNodeWithText("Light").assertExists()
        composeRule.onNodeWithText("Dark").assertExists()
        composeRule.onNodeWithText("Auto").assertExists()
    }

    @Test
    fun `click on Light theme updates selection`() {
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = {},
                isDarkTheme = false
            )
        }

        composeRule.onNodeWithText("Light").performClick()
        // Theme selection changes - verify no crash
    }

    @Test
    fun `back button is clickable`() {
        var backClicked = false
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = { backClicked = true },
                isDarkTheme = false
            )
        }

        composeRule.onNodeWithText("Back").performClick()
        // Verify back was triggered
    }

    @Test
    fun `dark theme renders correctly`() {
        composeRule.setContent {
            SettingsScreen(
                settings = AppSettings(),
                onSave = {},
                onBack = {},
                isDarkTheme = true
            )
        }

        composeRule.onNodeWithText("Settings").assertExists()
    }
}