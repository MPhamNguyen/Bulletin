package com.jdrms.bulletin.domain.messages.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ConversationBackSwipeDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private var returns = 0
    private lateinit var scroll: ScrollState

    @Test
    fun rightSwipeReturnsToInbox() {
        showChat()
        compose.onNodeWithTag("chat").performTouchInput {
            swipe(Offset(width * .15f, height * .5f), Offset(width * .85f, height * .5f), 400)
        }
        compose.runOnIdle { assertEquals(1, returns) }
    }

    @Test
    fun leftSwipeStaysInChat() {
        showChat()
        compose.onNodeWithTag("chat").performTouchInput {
            swipe(Offset(width * .85f, height * .5f), Offset(width * .15f, height * .5f), 400)
        }
        compose.runOnIdle { assertEquals(0, returns) }
    }

    @Test
    fun verticalScrollStaysInChat() {
        showChat()
        compose.onNodeWithTag("chat").performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertEquals(0, returns)
            assertTrue(scroll.value > 0)
        }
    }

    private fun showChat() {
        compose.setContent {
            scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().conversationBackSwipe { returns++ }.testTag("chat")) {
                Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Box(Modifier.height(3000.dp))
                }
            }
        }
    }
}
