package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MoneyInputDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun negativePasteShowsAnInlineErrorAndKeepsTheAcceptedAmount() {
        val digits = showInput()
        compose.onNodeWithText("Price").performTextInput("-1.00")

        compose.onNodeWithText("Amount cannot be negative.").assertIsDisplayed()
        compose.onNodeWithText("Price").assertTextContains("$1.25")
        compose.runOnIdle { assertEquals("125", digits.value) }

        compose.onNodeWithText("Price").performTextInput("9")
        compose.onNodeWithText("Amount cannot be negative.").assertDoesNotExist()
        compose.onNodeWithText("Price").assertTextContains("$12.59")
        compose.runOnIdle { assertEquals("1259", digits.value) }
    }

    @Test
    fun oversizedReplacementShowsAnInlineErrorAndKeepsTheAcceptedAmount() {
        val digits = showInput()
        compose.onNodeWithText("Price").performTextReplacement("10000000.00")

        compose.onNodeWithText("Amount cannot exceed $9,999,999.99.").assertIsDisplayed()
        compose.onNodeWithText("Price").assertTextContains("$1.25")
        compose.runOnIdle { assertEquals("125", digits.value) }
    }

    private fun showInput(): MutableStateFlow<String> {
        val digits = MutableStateFlow("125")
        compose.setContent {
            val current by digits.collectAsState()
            BulletinTheme {
                MoneyInput(digits = current, onDigitsChange = { digits.value = it }, label = { Text("Price") })
            }
        }
        return digits
    }
}
