package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarketplaceInterestNotificationRequestsTest {
    @Test
    fun keepsTapUntilSignInAndOnlyOpensForItsRecipient() {
        val requests = MarketplaceInterestNotificationRequests()
        val listing = MarketplaceItemId("listing-1")
        val recipient = MarketplaceInterestOwnerId("student-1")
        requests.submit(MarketplaceInterestNotificationRequest(listing, recipient))

        assertNull(requests.take(null))
        assertNull(requests.take(MarketplaceInterestOwnerId("student-2")))
        assertNull(requests.take(recipient))

        requests.submit(MarketplaceInterestNotificationRequest(listing, recipient))
        assertEquals(listing, requests.take(recipient))
        assertNull(requests.take(recipient))
    }
}
