package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.MessageSeller
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository

internal val actionListingId = HomeItemId("listing:original")
internal val actionMessage = SellerMessage.create("  Still available?  ").getOrThrow()

internal class HomeContactFixture {
    var listing = Listing(
        MarketplaceItemId(actionListingId.value), "private-seller-id", sellerName = "Private Seller",
        title = "Textbook", description = "A book", price = MarketplacePrice(10.0),
        category = MarketplaceCategory.TEXTBOOKS
    )
    var lookupFailure: Throwable? = null
    var lookupThrows: Throwable? = null
    var hidden = emptySet<String>()
    var reported = emptySet<MarketplaceItemId>()
    var available = true
    var sendFailure: Throwable? = null
    var sendThrows: Throwable? = null
    var senderFailure: Throwable? = null
    val sent = mutableListOf<Message>()
    val visibilityRequests = mutableListOf<MarketplacePageRequest>()
    val lookupIds = mutableListOf<String>()
    val marketplace = object : MarketplaceRepository by InMemoryMarketplaceRepository() {
        override suspend fun viewListing(listingID: String): Result<Listing> {
            lookupIds += listingID
            lookupThrows?.let { throw it }
            return lookupFailure?.let { Result.Error(it) } ?: Result.Success(listing)
        }
    }
    private val messages = object : MessagesRepository by InMemoryMessagesRepository() {
        override suspend fun sendMessage(message: Message): Result<Message> {
            sent += message
            sendThrows?.let { throw it }
            return sendFailure?.let { Result.Error(it) } ?: Result.Success(message)
        }
    }
    val visibility = GetVisibleMarketplaceListings(
        MarketplaceListingSource { request ->
            visibilityRequests += request
            val snapshot = MarketplaceListingSnapshot(
                listing.id.value, listing.sellerId, listing.sellerName, listing.title, listing.description,
                listing.price.amount, listing.price.currency, listing.category, listing.createdAtMillis
            )
            (if (available) listOf(snapshot) else emptyList()).pageFor(request)
        },
        MarketplaceBlockedUserProvider { hidden },
        MarketplaceReportedListingProvider { reported }
    )
    val adapter = MessagesHomeContactSellerAdapter(
        ViewMarketplaceListing(marketplace), visibility,
        MessageSeller(
            messages,
            CurrentMessageSenderProvider {
                senderFailure?.let { Result.Error(it) } ?: Result.Success(MessageSender(SenderId("viewer"), "Viewer"))
            }
        )
    )
}
