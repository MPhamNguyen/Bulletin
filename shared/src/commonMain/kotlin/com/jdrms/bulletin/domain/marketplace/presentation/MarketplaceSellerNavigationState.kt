package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

internal data class SellerListingContext(
    val listingId: String,
    val sellerId: String,
    val sellerName: String
)

internal data class MarketplaceSellerNavigationState(
    val destination: MarketplaceDestination = MarketplaceDestination.CATALOG,
    val listingContext: SellerListingContext? = null
) {
    fun navigate(
        destination: MarketplaceDestination,
        openedListing: SellerListingContext? = null
    ): MarketplaceSellerNavigationState = copy(
        destination = destination,
        listingContext = when {
            destination == MarketplaceDestination.CATALOG -> null
            openedListing != null -> openedListing
            else -> listingContext
        }
    )
}

internal fun Listing.toSellerListingContext() = SellerListingContext(
    listingId = id.value,
    sellerId = sellerId,
    sellerName = sellerName
)

internal fun MarketplaceItem.toSellerListingContext() = SellerListingContext(
    listingId = id.value,
    sellerId = sellerId,
    sellerName = sellerName
)
