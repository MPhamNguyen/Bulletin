package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilterMode
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

internal data class MarketplaceAttributeFilterInput(
    val exact: String = "",
    val min: String = "",
    val max: String = ""
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

    fun toFilters(category: MarketplaceCategory): Result {
        val errors = mutableMapOf<String, String>()
        val selectedChoices = selectedChoices()
        val predicates = MarketplaceAttributeCatalog.applicableSpecs(category, selectedChoices).mapNotNull { spec ->
            val input = input(spec.key)
            runCatching { predicate(spec, input, selectedChoices) }
                .getOrElse {
                    errors[spec.key] = errorMessage(spec)
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

    private fun predicate(
        spec: MarketplaceAttributeSpec,
        input: MarketplaceAttributeFilterInput,
        selectedChoices: Map<String, String>
    ): MarketplaceAttributePredicate? = when (spec.kind) {
        MarketplaceAttributeKind.TEXT -> textPredicate(spec.key, input.exact)
        MarketplaceAttributeKind.CHOICE -> input.exact.takeIf(String::isNotBlank)?.also { code ->
            require(code in spec.choicesFor(selectedChoices))
        }?.let(MarketplaceAttributePredicate::Choice)
        MarketplaceAttributeKind.INTEGER -> when (spec.filterMode) {
            MarketplaceAttributeFilterMode.EXACT -> input.exact.takeIf(String::isNotBlank)?.let { raw ->
                val value = requireNotNull(raw.trim().toLongOrNull())
                MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(value, value))
            }
            MarketplaceAttributeFilterMode.RANGE -> integerPredicate(input)
        }
        MarketplaceAttributeKind.DECIMAL -> decimalPredicate(input)
        MarketplaceAttributeKind.DATE -> datePredicate(input)
    }

    private fun selectedChoices(): Map<String, String> = inputs.mapNotNull { (key, input) ->
        input.exact.trim().takeIf(String::isNotEmpty)?.let { key to it }
    }.toMap()

    private fun textPredicate(key: String, raw: String): MarketplaceAttributePredicate? {
        if (raw.isBlank()) return null
        val text = requireNotNull(MarketplaceAttributeMatching.exactText(key, raw))
        if (key == MarketplaceAttributeCatalog.BRAND) {
            require(text.length <= MarketplaceAttributeCatalog.MAX_BRAND_LENGTH)
            return MarketplaceAttributePredicate.ExactText(text, usesMatchKey = true)
        }
        require(text.length <= 60)
        return MarketplaceAttributePredicate.ExactText(text)
    }

    private fun integerPredicate(input: MarketplaceAttributeFilterInput): MarketplaceAttributePredicate? {
        if (input.min.isBlank() && input.max.isBlank()) return null
        val min = input.min.trim().takeIf(String::isNotEmpty)?.toLongOrNull()
        val max = input.max.trim().takeIf(String::isNotEmpty)?.toLongOrNull()
        require(input.min.isBlank() || min != null)
        require(input.max.isBlank() || max != null)
        return MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(min, max))
    }

    private fun decimalPredicate(input: MarketplaceAttributeFilterInput): MarketplaceAttributePredicate? {
        if (input.min.isBlank() && input.max.isBlank()) return null
        val min = input.min.trim().takeIf(String::isNotEmpty)?.toDoubleOrNull()
        val max = input.max.trim().takeIf(String::isNotEmpty)?.toDoubleOrNull()
        require(input.min.isBlank() || min != null)
        require(input.max.isBlank() || max != null)
        return MarketplaceAttributePredicate.DecimalRange(min, max)
    }

    private fun datePredicate(input: MarketplaceAttributeFilterInput): MarketplaceAttributePredicate? {
        if (input.min.isBlank() && input.max.isBlank()) return null
        return MarketplaceAttributePredicate.DateRange(
            input.min.trim().takeIf(String::isNotEmpty),
            input.max.trim().takeIf(String::isNotEmpty)
        )
    }

    private fun errorMessage(spec: MarketplaceAttributeSpec): String = when {
        spec.kind == MarketplaceAttributeKind.INTEGER && spec.filterMode == MarketplaceAttributeFilterMode.EXACT ->
            "Enter a valid whole number."
        spec.kind == MarketplaceAttributeKind.TEXT -> "Enter a valid exact value."
        spec.kind == MarketplaceAttributeKind.CHOICE -> "Choose a valid option."
        spec.kind == MarketplaceAttributeKind.INTEGER ->
            "Use whole numbers, with the minimum no larger than the maximum."
        spec.kind == MarketplaceAttributeKind.DECIMAL ->
            "Use positive numbers, with the minimum no larger than the maximum."
        else -> "Use YYYY-MM-DD, with the start date no later than the end date."
    }

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
    is MarketplaceAttributePredicate.DecimalRange -> MarketplaceAttributeFilterInput(
        min = min?.toString().orEmpty(),
        max = max?.toString().orEmpty()
    )
    is MarketplaceAttributePredicate.DateRange -> MarketplaceAttributeFilterInput(
        min = minIso.orEmpty(),
        max = maxIso.orEmpty()
    )
}
