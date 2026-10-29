package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.domain.profile.presentation.SignInEmailField
import com.jdrms.bulletin.domain.profile.presentation.SignInPasswordField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@OptIn(ExperimentalLayoutApi::class)
@RunWith(AndroidJUnit4::class)
class BulletinTextFieldDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var imeVisible = false
    private var keyboard: SoftwareKeyboardController? = null

    @Test fun signInOutsideTapDismissesAndRetapRestoresKeyboard() {
        compose.setContent {
            BulletinTheme {
                imeVisible = WindowInsets.isImeVisible
                keyboard = LocalSoftwareKeyboardController.current
                Column(Modifier.fillMaxSize().dismissKeyboardOnTapOutside()) {
                    var email by remember { mutableStateOf("") }
                    SignInEmailField(email, { email = it }, {})
                    SignInPasswordField("", {}, {})
                    Spacer(Modifier.fillMaxWidth().weight(1f).testTag("outside"))
                }
            }
        }
        val email = compose.onNodeWithText("you@university.edu")
        email.performTouchInput { click() }
        compose.waitUntil(5000) { imeVisible }
        email.performTextInput("student@example.edu")
        compose.onNodeWithTag("outside").performTouchInput { click() }
        compose.waitUntil(5000) { !imeVisible }
        compose.onNodeWithText("student@example.edu").assertIsNotFocused().performTouchInput { click() }
        compose.waitUntil(5000) { imeVisible }
        compose.runOnIdle { keyboard?.hide() }
        compose.waitUntil(5000) { !imeVisible }
        compose.onNodeWithText("student@example.edu").assertIsFocused().performTouchInput { click() }
        compose.waitUntil(5000) { imeVisible }
        compose.onNodeWithText("student@example.edu").performImeAction()
        compose.waitUntil(5000) { !imeVisible }
        compose.onNodeWithText("student@example.edu").assertIsNotFocused()
    }

    @Test fun consumedButtonTapPreservesFocusAndCustomDoneRuns() {
        var doneCount = 0
        var clicks = 0
        compose.setContent {
            BulletinTheme {
                Column(Modifier.fillMaxSize().dismissKeyboardOnTapOutside()) {
                    BulletinTextField(
                        value = "draft",
                        onValueChange = {},
                        modifier = Modifier.testTag("input"),
                        singleLine = true,
                        keyboardActions = KeyboardActions(onDone = { doneCount++ })
                    )
                    Button(onClick = { clicks++ }, modifier = Modifier.height(60.dp)) { Text("Action") }
                }
            }
        }
        compose.onNodeWithTag("input").performTouchInput { click() }
        compose.onNodeWithText("Action").performTouchInput { click() }
        compose.onNodeWithTag("input").assertIsFocused().performImeAction()
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(1, doneCount)
        }
    }
}
