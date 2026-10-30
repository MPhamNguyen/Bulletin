package com.jdrms.bulletin.domain.listings.domain.model

/**
 * Optional item facts for a listing.
 *
 * [values] holds recognized, valid, typed entries. [preserved] holds raw JSON text for stored entries this client
 * cannot interpret: unknown keys from a newer client, retired keys, keys that do not apply to the listing's
 * category, and values that fail validation. Preserved entries are never shown or edited, and the write path
 * carries them back unchanged so an unrelated edit cannot erase them.
 */
data class ListingAttributes(
    val values: Map<String, ListingAttributeValue> = emptyMap(),
    val preserved: Map<String, String> = emptyMap()
) {
    val size: Int get() = values.size + preserved.size

    fun isEmpty(): Boolean = values.isEmpty() && preserved.isEmpty()

    operator fun get(key: String): ListingAttributeValue? = values[key]

    fun with(key: String, value: ListingAttributeValue): ListingAttributes =
        copy(values = values + (key to value), preserved = preserved - key)

    fun without(key: String): ListingAttributes = copy(values = values - key)

    fun withPreserved(preserved: Map<String, String>): ListingAttributes = copy(preserved = preserved)

    companion object {
        val EMPTY = ListingAttributes()
    }
}
