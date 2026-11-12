package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

internal data class MarketplaceAttributeFilterInput(
    val exact: String = "",
    val min: String = "",
    val max: String = "",
    val capacities: Set<MarketplaceCapacityRange> = emptySet()
)

/** Category-driven form text for typed marketplace attribute filters. */
internal data class MarketplaceAttributeFilterDraft(
    val inputs: Map<String, MarketplaceAttributeFilterInput> = emptyMap()
) {
    data class Result(val filters: MarketplaceAttributeFilters?, val errors: Map<String, String>)

    fun input(key: String): MarketplaceAttributeFilterInput = inputs[key] ?: MarketplaceAttributeFilterInput()

    fun withExact(key: String, value: String) = withInput(key, input(key).copy(exact = value))
    fun withMin(key: String, value: String) = withInput(key, input(key).copy(min = value))
    fun withMax(key: String, value: String) = withInput(key, input(key).copy(max = value))

    fun toggleCapacity(key: String, range: MarketplaceCapacityRange): MarketplaceAttributeFilterDraft {
        val current = input(key).selectedCapacities(key)
        val selected = if (range in current) current - range else current + range
        return withInput(key, MarketplaceAttributeFilterInput(capacities = selected))
    }

    fun toFilters(category: MarketplaceCategory): Result {
        val errors = mutableMapOf<String, String>()
        val selectedChoices = selectedChoices()
        val predicates = MarketplaceAttributeCatalog.applicableSpecs(category, selectedChoices).mapNotNull { spec ->
            val input = input(spec.key)
            runCatching { MarketplaceAttributeFilterParser.predicate(spec, input, selectedChoices) }
                .getOrElse {
                    errors[spec.key] = MarketplaceAttributeFilterParser.errorMessage(spec)
                    null
                }
                ?.let { spec.key to it }
        }.toMap()
        return if (errors.isEmpty()) {
            Result(MarketplaceAttributeFilters(predicates = predicates), emptyMap())
        } else {
            Result(null, errors)
        }
    }

    fun reconciled(category: MarketplaceCategory): MarketplaceAttributeFilterDraft {
        val selectedChoices = selectedChoices()
        val visible = MarketplaceAttributeCatalog.applicableSpecs(category, selectedChoices).associateBy { it.key }
        val reconciled = inputs.filter { (key, input) ->
            val spec = visible[key] ?: return@filter false
            if (spec.kind != MarketplaceAttributeKind.CHOICE || input.exact.isBlank()) return@filter true
            input.exact in spec.choicesFor(selectedChoices)
        }
        return copy(inputs = reconciled)
    }

    private fun withInput(key: String, value: MarketplaceAttributeFilterInput) = copy(inputs = inputs + (key to value))

    private fun selectedChoices(): Map<String, String> = inputs.mapNotNull { (key, input) ->
        input.exact.trim().takeIf(String::isNotEmpty)?.let { key to it }
    }.toMap()

    companion object {
        fun from(filters: MarketplaceAttributeFilters): MarketplaceAttributeFilterDraft {
            val inputs = filters.allPredicates.mapValues { (_, predicate) -> predicate.toInput() }
            return MarketplaceAttributeFilterDraft(inputs)
        }
    }
}

private fun MarketplaceAttributePredicate.toInput(): MarketplaceAttributeFilterInput = when (this) {
    is MarketplaceAttributePredicate.ExactText -> MarketplaceAttributeFilterInput(exact = value)
    is MarketplaceAttributePredicate.Choice -> MarketplaceAttributeFilterInput(exact = code)
    is MarketplaceAttributePredicate.IntegerRange -> MarketplaceAttributeFilterInput(
        exact = range.min?.takeIf { it == range.max }?.toString().orEmpty(),
        min = range.min?.toString().orEmpty(),
        max = range.max?.toString().orEmpty()
    )
    is MarketplaceAttributePredicate.IntegerOptions -> MarketplaceAttributeFilterInput(capacities = ranges)
    is MarketplaceAttributePredicate.DecimalRange -> MarketplaceAttributeFilterInput(
        min = min?.toString().orEmpty(),
        max = max?.toString().orEmpty()
    )
    is MarketplaceAttributePredicate.DateRange -> MarketplaceAttributeFilterInput(
        min = minIso.orEmpty(),
        max = maxIso.orEmpty()
    )
}
