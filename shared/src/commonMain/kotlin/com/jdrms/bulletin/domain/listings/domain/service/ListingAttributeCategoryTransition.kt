package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

data class DiscardedListingAttribute(val key: String, val label: String)

data class ListingAttributeTransition(
    /** Draft inputs to submit under the new category: only compatible, still-applicable, nonblank values. */
    val retained: Map<String, String>,
    /** Nonblank values the seller typed that the new category cannot keep. */
    val discarded: List<DiscardedListingAttribute>
) {
    val discardsValues: Boolean get() = discarded.isNotEmpty()
}

/**
 * Plans what happens to form input when the seller switches category. A value survives only when the new
 * category defines the same key with the same type and the value is still applicable there.
 */
object ListingAttributeCategoryTransition {
    fun plan(from: ListingCategory, to: ListingCategory, inputs: Map<String, String>): ListingAttributeTransition {
        val oldDefinitions = ListingAttributeCatalog.definitionsFor(from).associateBy { it.key }
        val newDefinitions = ListingAttributeCatalog.definitionsFor(to).associateBy { it.key }
        // Only fields the seller could see count: a dependent field hidden by its controlling choice is not
        // submitted today, so it is neither retained nor reported as discarded.
        val visible = inputs.filter { (key, value) ->
            value.isNotBlank() && oldDefinitions[key]?.let { it.applicability?.isSatisfiedBy(inputs) ?: true } == true
        }
        val typeCompatible = visible.filter { (key, _) ->
            newDefinitions[key]?.isCompatibleWith(oldDefinitions.getValue(key)) == true
        }
        val retained = typeCompatible.filterKeys { key ->
            newDefinitions.getValue(key).applicability?.isSatisfiedBy(typeCompatible) ?: true
        }
        val discarded = visible.keys
            .filter { it !in retained }
            .map { key -> DiscardedListingAttribute(key, oldDefinitions.getValue(key).label) }
        return ListingAttributeTransition(retained, discarded)
    }
}
