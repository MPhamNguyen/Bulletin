package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HomeActionsIntegrationTest {
    @Test
    fun wiredSaveAndContactUseExistingContextsWithoutRecordingDecisions() = runTest {
        val container = AppContainer(isInspectionMode = true)
        val viewer = container.authRepository.login(
            StudentEmail("dominic.alfonso@student.csulb.edu"),
            "password123"
        ).getOrThrow()
        container.sessionRepository.authenticated(viewer)
        val listing = container.marketplaceRepository.getCatalog().first { it.sellerId != viewer.id.value }
        val id = HomeItemId(listing.id.value)
        assertIs<Result.Success<Unit>>(container.homeSaveListing.save(viewer.id.value, id))
        assertTrue(listing.id in container.getMarketplaceListingBookmarks.getBookmarkedIds(viewer.id.value))
        assertIs<Result.Success<Unit>>(container.sendQuickMessage(viewer.id.value, id, "  Still available?  "))
        val conversations = container.getConversations().getOrThrow().conversations
        assertEquals(1, conversations.size)
        assertEquals(id.value, conversations.single().listingId?.value)
        assertEquals("Still available?", conversations.single().lastMessage?.content)
        SwipeVerdict.entries.forEach { verdict ->
            assertEquals(
                emptySet(),
                container.swipeDecisionRepository.getExcludedListingIds(viewer.id.value, verdict).getOrThrow()
            )
        }
        container.recordSwipeDecision(viewer.id.value, SwipeDecision(id, SwipeVerdict.SAVE)).getOrThrow()
        assertEquals(
            setOf(id),
            container.swipeDecisionRepository.getExcludedListingIds(viewer.id.value, SwipeVerdict.SAVE).getOrThrow()
        )
    }
}
