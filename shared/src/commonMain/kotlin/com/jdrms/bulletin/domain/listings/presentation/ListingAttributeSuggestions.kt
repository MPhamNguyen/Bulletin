package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeText

/** Free-text suggestions. They only fill the field; any spelling the seller types is still accepted. */
internal object ListingAttributeSuggestions {
    private const val MAX_SUGGESTIONS = 4

    private val electronicsBrands = listOf(
        "Apple", "ASUS", "Dell", "Framework", "HP", "Lenovo", "Logitech", "Microsoft", "Samsung", "Sony"
    )
    private val clothingBrands = listOf(
        "Adidas", "Carhartt", "Champion", "H&M", "Levi's", "Nike", "Patagonia", "Uniqlo", "Zara"
    )

    fun brandSuggestions(category: ListingCategory, typed: String): List<String> {
        val pool = when (category) {
            ListingCategory.ELECTRONICS -> electronicsBrands
            ListingCategory.CLOTHING -> clothingBrands
            else -> emptyList()
        }
        val query = ListingAttributeText.matchKey(typed)
        return pool.filter { brand ->
            val key = ListingAttributeText.matchKey(brand)
            key != query && key.startsWith(query)
        }.take(MAX_SUGGESTIONS)
    }

    fun suggestionsFor(category: ListingCategory, key: String, typed: String): List<String> =
        if (key == ListingAttributeKeys.BRAND) brandSuggestions(category, typed) else emptyList()
}
