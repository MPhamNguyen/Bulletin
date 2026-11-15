package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeOption

/** Form text for a multi-select attribute: stable option codes joined by commas. Codes never contain commas. */
object ListingMultiChoiceInput {
    private const val SEPARATOR = ","

    fun decode(raw: String): List<String> = raw.split(SEPARATOR).map(String::trim).filter(String::isNotEmpty).distinct()

    fun encode(codes: Collection<String>): String = codes.joinToString(SEPARATOR)

    /** Adds [code] when absent and removes it when present, keeping the catalog order of [options]. */
    fun toggle(raw: String, code: String, options: List<ListingAttributeOption>): String {
        val selected = decode(raw).toMutableSet()
        if (!selected.add(code)) selected.remove(code)
        return encode(options.map { it.code }.filter { it in selected })
    }
}
