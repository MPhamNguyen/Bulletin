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
    fun chosenBuyerSeesSellerAndListing() {
        assertEquals("Alex sold Desk to you", buyerSaleNotice(listingSalePreview(sold = true), buyer, seller))
    }

    @Test
    fun activeOrRestoredListingHasNoNotice() {
        assertNull(buyerSaleNotice(listingSalePreview(), buyer, seller))
    }

    @Test
    fun otherBuyersAndSellerHaveNoNotice() {
        val preview = listingSalePreview(sold = true)
        assertNull(buyerSaleNotice(preview, SenderId("other"), seller))
        assertNull(buyerSaleNotice(preview, seller.id, ConversationParticipant(buyer, "Buyer")))
        assertNull(buyerSaleNotice(preview.copy(soldToBuyerId = null), buyer, seller))
    }

    @Test
    fun missingIdentityOrDifferentCounterpartHasNoNotice() {
        val preview = listingSalePreview(sold = true)
        assertNull(buyerSaleNotice(null, buyer, seller))
        assertNull(buyerSaleNotice(preview, null, seller))
        assertNull(buyerSaleNotice(preview, buyer, null))
        assertNull(buyerSaleNotice(preview, buyer, seller.copy(id = SenderId("other"))))
    }
}
