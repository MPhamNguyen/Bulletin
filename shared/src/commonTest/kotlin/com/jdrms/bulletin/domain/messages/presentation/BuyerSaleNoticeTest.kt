package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuyerSaleNoticeTest {
    private val buyer = SenderId("buyer")
    private val seller = ConversationParticipant(SenderId("seller"), "Alex")

    @Test
    fun chosenBuyerIsPromptedToRateSeller() {
        assertEquals("Alex", buyerRatingPromptSeller(listingSalePreview(sold = true), buyer, seller))
    }

    @Test
    fun activeOrRestoredListingHasNoNotice() {
        assertNull(buyerRatingPromptSeller(listingSalePreview(), buyer, seller))
    }

    @Test
    fun otherBuyersAndSellerHaveNoNotice() {
        val preview = listingSalePreview(sold = true)
        assertNull(buyerRatingPromptSeller(preview, SenderId("other"), seller))
        assertNull(buyerRatingPromptSeller(preview, seller.id, ConversationParticipant(buyer, "Buyer")))
        assertNull(buyerRatingPromptSeller(preview.copy(soldToBuyerId = null), buyer, seller))
    }

    @Test
    fun missingIdentityOrDifferentCounterpartHasNoNotice() {
        val preview = listingSalePreview(sold = true)
        assertNull(buyerRatingPromptSeller(null, buyer, seller))
        assertNull(buyerRatingPromptSeller(preview, null, seller))
        assertNull(buyerRatingPromptSeller(preview, buyer, null))
        assertNull(buyerRatingPromptSeller(preview, buyer, seller.copy(id = SenderId("other"))))
    }
}
