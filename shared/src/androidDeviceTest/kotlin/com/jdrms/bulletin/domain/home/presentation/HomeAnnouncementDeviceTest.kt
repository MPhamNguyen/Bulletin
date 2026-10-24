package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class HomeAnnouncementDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @Test fun confirmedSaveAndPassAnnounceOnceDespiteRecomposition() {
        harness.show(compose)
        compose.runOnIdle { harness.actions.saveCurrentCard() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("Saved", harness.announcements.messages.tryReceive().getOrNull())
            harness.tick++
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNull(harness.announcements.messages.tryReceive().getOrNull())
            harness.actions.passCurrentCard()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("Passed", harness.announcements.messages.tryReceive().getOrNull())
            assertNull(harness.announcements.messages.tryReceive().getOrNull())
        }
    }

    @Test fun sentReceiptAnnouncesOnceAndDoesNotAnnounceSaved() {
        harness.show(compose)
        compose.runOnIdle {
            harness.actions.openTray()
            harness.actions.onDraftChanged("hello")
        }
        compose.waitForIdle()
        compose.runOnIdle { harness.actions.sendMessage() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("Message sent", harness.announcements.messages.tryReceive().getOrNull())
            harness.tick++
        }
        compose.waitForIdle()
        compose.runOnIdle { assertNull(harness.announcements.messages.tryReceive().getOrNull()) }
    }

    @Test fun sendFailureAnnouncesOnceAndAgainForAnExplicitNewAttempt() {
        harness.ports.sendResult = { Result.Error(ContactSellerFailure.Network()) }
        harness.show(compose)
        compose.runOnIdle {
            harness.actions.openTray()
            harness.actions.onDraftChanged("hello")
        }
        compose.waitForIdle()
        compose.runOnIdle { harness.actions.sendMessage() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("Message not sent. Try again.", harness.announcements.messages.tryReceive().getOrNull())
            harness.tick++
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNull(harness.announcements.messages.tryReceive().getOrNull())
            harness.actions.sendMessage()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("Message not sent. Try again.", harness.announcements.messages.tryReceive().getOrNull())
            assertNull(harness.announcements.messages.tryReceive().getOrNull())
        }
    }

    @Test fun announcementRegionHasPoliteSemantics() {
        compose.setContent { HomeAnnouncementRegion(HomeAnnouncements(), HomeInteractionState(accountNotice = null)) }
        compose.onNodeWithTag("home_announcement").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )
    }

    @Test fun guestNoticeAnnouncesOnce() {
        val announcements = HomeAnnouncements()
        var tick by mutableIntStateOf(0)
        compose.setContent {
            @Suppress("UNUSED_VARIABLE")
            val recompose = tick
            HomeStateAnnouncements(announcements, HomeInteractionState())
        }
        compose.runOnIdle {
            assertEquals(
                "Sign in to save listings or message sellers.",
                announcements.messages.tryReceive().getOrNull()
            )
            tick++
        }
        compose.waitForIdle()
        compose.runOnIdle { assertNull(announcements.messages.tryReceive().getOrNull()) }
    }
}
