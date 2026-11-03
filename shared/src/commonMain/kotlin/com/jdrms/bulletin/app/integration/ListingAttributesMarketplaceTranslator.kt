package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes

/**
 * Context boundary: turns the listings-owned attribute aggregate into marketplace's read vocabulary. Preserved
 * entries (unknown, retired, invalid) stay behind; only recognized typed values cross.
 */
internal fun ListingAttributes.toMarketplaceAttributes(): MarketplaceAttributes =
    MarketplaceAttributes(
        values.mapValues { (_, value) ->
            when (value) {
                is ListingAttributeValue.Text -> MarketplaceAttributeValue.Text(value.value)
                is ListingAttributeValue.Choice -> MarketplaceAttributeValue.Choice(value.code)
                is ListingAttributeValue.Integer -> MarketplaceAttributeValue.Integer(value.value)
                is ListingAttributeValue.Decimal -> MarketplaceAttributeValue.Decimal(value.value.toPlainString())
                is ListingAttributeValue.Date -> MarketplaceAttributeValue.Date(value.value.toString())
            }
        }
    )
