package com.tuneflow.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_showsRequiredFields() {
        rule.onNodeWithText("Navidrome URL").assertIsDisplayed()
        rule.onNodeWithText("Username").assertIsDisplayed()
        rule.onNodeWithText("Password").assertIsDisplayed()
    }
}
