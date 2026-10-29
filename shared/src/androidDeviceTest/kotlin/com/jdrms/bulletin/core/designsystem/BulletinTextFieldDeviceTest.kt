package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BulletinTextFieldDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var clicks = 0
    private var imeVisible = false
    private var inputFocused = false

    @Test fun inputFocusTracksFieldSwitchingAndOutsideTap() {
        show()
        compose.runOnIdle { assertEquals(false, inputFocused) }
        compose.onNodeWithTag("first").performClick()
        compose.runOnIdle { assertEquals(true, inputFocused) }
        compose.onNodeWithTag("second").performTouchInput { click() }
        compose.runOnIdle { assertEquals(true, inputFocused) }
        compose.onNodeWithTag("outside").performTouchInput { click() }
        compose.runOnIdle { assertEquals(false, inputFocused) }
    }

    @Test fun disposedDialogReleasesAppInputFocus() {
        var open by mutableStateOf(true)
        compose.setContent {
            BulletinTheme {
                BulletinInputHost {
                    val focused = LocalBulletinInputFocus.current?.isFocused == true
                    SideEffect { inputFocused = focused }
                    if (open) {
                        Dialog(onDismissRequest = {}) {
                            BulletinInputHost { Inputs() }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("first").performClick()
        compose.runOnIdle {
            assertEquals(true, inputFocused)
            open = false
        }
        compose.runOnIdle { assertEquals(false, inputFocused) }
    }

    @Test fun outsideTapClearsFocusAndPreservesText() {
        show()
        compose.onNodeWithTag("first").performClick().performTextInput("kept")
        compose.onNodeWithTag("outside").performTouchInput { click() }
        compose.onNodeWithTag("first").assertIsNotFocused().assertTextEquals("kept")
    }

    @Test fun tappingButtonClearsFocusAndStillInvokesAction() {
        show()
        compose.onNodeWithTag("first").performClick()
        compose.onNodeWithText("Action").performTouchInput { click() }
        compose.onNodeWithTag("first").assertIsNotFocused()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun tappingAnotherFieldTransfersFocusAndInsideTapKeepsFocus() {
        show()
        compose.onNodeWithTag("first").performClick()
        compose.onNodeWithTag("second").performTouchInput { click() }
        compose.onNodeWithTag("first").assertIsNotFocused()
        compose.onNodeWithTag("second").assertIsFocused().performTextInput("second")
        compose.onNodeWithTag("second").performTouchInput { click() }
        compose.onNodeWithTag("second").assertIsFocused().assertTextEquals("second")
    }

    @Test fun defaultDoneActionClearsFocus() {
        show()
        compose.onNodeWithTag("first").performClick().performImeAction()
        compose.onNodeWithTag("first").assertIsNotFocused()
    }

    @Test fun dialogHostDismissesItsInputFocus() {
        compose.setContent {
            BulletinTheme {
                BulletinInputHost {
                    Dialog(onDismissRequest = {}) {
                        BulletinInputHost { Inputs() }
                    }
                }
            }
        }
        compose.onNodeWithTag("first").performClick()
        compose.onNodeWithTag("outside").performTouchInput { click() }
        compose.onNodeWithTag("first").assertIsNotFocused()
    }

    @Test fun outsideGestureDoesNotBlockScrolling() {
        val scroll = androidx.compose.foundation.ScrollState(0)
        compose.setContent {
            BulletinTheme {
                BulletinInputHost(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).testTag("scroll")) {
                        Inputs()
                        Spacer(Modifier.height(1500.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("first").performClick()
        compose.onNodeWithTag("scroll").performTouchInput { swipeUp() }
        compose.runOnIdle { kotlin.test.assertTrue(scroll.value > 0) }
    }

    @Test fun outsideTapHidesTheSoftwareKeyboard() {
        show()
        compose.onNodeWithTag("first").performClick()
        compose.waitUntil(5000) { imeVisible }
        compose.onNodeWithTag("outside").performTouchInput { click() }
        compose.waitUntil(5000) { !imeVisible }
        compose.onNodeWithTag("first").assertIsNotFocused()
    }

    @Test fun customSendActionStillRuns() {
        compose.setContent {
            BulletinTheme {
                BulletinInputHost {
                    BulletinTextField("message", {}, Modifier.testTag("send")) { field ->
                        field.keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                        field.keyboardActions = KeyboardActions(onSend = { clicks++ })
                    }
                }
            }
        }
        compose.onNodeWithTag("send").performClick().performImeAction()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    private fun show() {
        compose.setContent { BulletinTheme { BulletinInputHost(Modifier.fillMaxSize()) { Inputs() } } }
    }

    @Composable
    private fun Inputs() {
        val visible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val focused = LocalBulletinInputFocus.current?.isFocused == true
        SideEffect {
            imeVisible = visible
            inputFocused = focused
        }
        var text by remember { mutableStateOf("") }
        var selectedText by remember { mutableStateOf(TextFieldValue("", TextRange.Zero)) }
        Column {
            BulletinTextField(text, { text = it }, Modifier.testTag("first")) { it.singleLine = true }
            BulletinTextField(selectedText, { selectedText = it }, Modifier.testTag("second"))
            Button(onClick = { clicks++ }) { Text("Action") }
            Spacer(Modifier.size(70.dp).testTag("outside"))
        }
    }
}
