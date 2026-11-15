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
        values.mapNotNull { (key, value) -> toMarketplaceValue(value)?.let { key to it } }.toMap()
    )

/** Marketplace has no multi-value vocabulary yet, so a multi-select does not cross the boundary. */
private fun toMarketplaceValue(value: ListingAttributeValue): MarketplaceAttributeValue? = when (value) {
    is ListingAttributeValue.Text -> MarketplaceAttributeValue.Text(value.value)
    is ListingAttributeValue.Choice -> MarketplaceAttributeValue.Choice(value.code)
    is ListingAttributeValue.MultiChoice -> null
    is ListingAttributeValue.Integer -> MarketplaceAttributeValue.Integer(value.value)
    is ListingAttributeValue.Decimal -> MarketplaceAttributeValue.Decimal(value.value.toPlainString())
    is ListingAttributeValue.Date -> MarketplaceAttributeValue.Date(value.value.toString())
}
