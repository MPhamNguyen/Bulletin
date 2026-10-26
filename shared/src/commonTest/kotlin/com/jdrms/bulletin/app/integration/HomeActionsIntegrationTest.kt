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
        val viewer = container.profile.authRepository.login(
            StudentEmail("dominic.alfonso@student.csulb.edu"),
            "password123"
        ).getOrThrow()
        container.profile.sessionRepository.authenticated(viewer)
        val listing = container.marketplace.marketplaceRepository.getCatalog().first { it.sellerId != viewer.id.value }
        val id = HomeItemId(listing.id.value)
        assertIs<Result.Success<Unit>>(container.home.homeSaveListing.save(viewer.id.value, id))
        assertTrue(listing.id in container.marketplace.getMarketplaceListingBookmarks.getBookmarkedIds(viewer.id.value))
        assertIs<Result.Success<Unit>>(container.home.sendQuickMessage(viewer.id.value, id, "  Still available?  "))
        val conversations = container.messages.getConversations().getOrThrow().conversations
        assertEquals(1, conversations.size)
        assertEquals(id.value, conversations.single().listingId?.value)
        assertEquals("Still available?", conversations.single().lastMessage?.content)
        SwipeVerdict.entries.forEach { verdict ->
            assertEquals(
                emptySet(),
                container.home.swipeDecisionRepository.getExcludedListingIds(viewer.id.value, verdict).getOrThrow()
            )
        }
        container.home.recordSwipeDecision(viewer.id.value, SwipeDecision(id, SwipeVerdict.SAVE)).getOrThrow()
        assertEquals(
            setOf(id),
            container.home.swipeDecisionRepository.getExcludedListingIds(
                viewer.id.value,
                SwipeVerdict.SAVE
            ).getOrThrow()
        )
    }
}
