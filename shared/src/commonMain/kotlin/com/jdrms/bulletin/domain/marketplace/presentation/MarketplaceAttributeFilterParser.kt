package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilterMode
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange

/** Converts filter form values into validated attribute predicates. */
internal object MarketplaceAttributeFilterParser {
    fun predicate(
        spec: MarketplaceAttributeSpec,
        input: MarketplaceAttributeFilterInput,
        selectedChoices: Map<String, String>
    ): MarketplaceAttributePredicate? {
        if (input.capacities.isNotEmpty()) {
            require(input.capacities.all { it in MarketplaceCapacityOptions.forKey(spec.key) })
            return MarketplaceAttributePredicate.IntegerOptions(input.capacities)
        }
        return when (spec.kind) {
            MarketplaceAttributeKind.TEXT -> textPredicate(spec.key, input.exact)
            MarketplaceAttributeKind.CHOICE -> choicePredicate(spec, input, selectedChoices)
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
    }

    private fun choicePredicate(
        spec: MarketplaceAttributeSpec,
        input: MarketplaceAttributeFilterInput,
        selectedChoices: Map<String, String>
    ): MarketplaceAttributePredicate? {
        if (input.choices.isNotEmpty()) {
            require(spec.key == MarketplaceAttributeCatalog.COLOR)
            require(input.choices.all { it in spec.choicesFor(selectedChoices) })
            return MarketplaceAttributePredicate.ChoiceOptions(input.choices)
        }
        return input.exact.takeIf(String::isNotBlank)?.also { code ->
            require(code in spec.choicesFor(selectedChoices))
        }?.let(MarketplaceAttributePredicate::Choice)
    }

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

    fun errorMessage(spec: MarketplaceAttributeSpec): String = when {
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
}
