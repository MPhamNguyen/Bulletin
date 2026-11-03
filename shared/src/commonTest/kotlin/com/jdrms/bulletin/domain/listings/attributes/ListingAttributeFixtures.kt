package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

internal val laptopAttributes = ListingAttributes(
    mapOf(
        "brand" to ListingAttributeValue.Text("Framework"),
        "device_type" to ListingAttributeValue.Choice("laptop"),
        "ram_gb" to ListingAttributeValue.Integer(16),
        "storage_gb" to ListingAttributeValue.Integer(512)
    )
)

internal val housingAttributes = ListingAttributes(
    mapOf(
        "monthly_rent_usd" to ListingAttributeValue.Decimal(ListingDecimal.parse("1250.50", 2)!!),
        "available_date" to ListingAttributeValue.Date(ListingAttributeDate(2026, 9, 1)),
        "housing_type" to ListingAttributeValue.Choice("room")
    )
)

internal fun attributeListing(
    id: String = "listing-1",
    seller: String = "seller-1",
    category: ListingCategory = ListingCategory.ELECTRONICS,
    attributes: ListingAttributes = laptopAttributes
) = Listing(
    id = ListingId(id),
    sellerId = SellerId(seller),
    sellerName = "Seller",
    title = "Framework laptop",
    description = "Works great.",
    price = ListingPrice(800.0),
    category = category,
    attributes = attributes
)
