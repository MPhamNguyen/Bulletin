package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeApplicability
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

/** Validates typed attributes against the catalog. Only supplied optional values are checked. */
class ListingAttributeValidationPolicy {
    fun validate(category: ListingCategory, attributes: ListingAttributes): List<ListingAttributeIssue> {
        val issues = mutableListOf<ListingAttributeIssue>()
        if (attributes.size > ListingAttributeLimits.MAX_ATTRIBUTE_COUNT) {
            issues += ListingAttributeIssue(
                null,
                "A listing can have at most ${ListingAttributeLimits.MAX_ATTRIBUTE_COUNT} attributes."
            )
        }
        if (estimatedPayloadBytes(attributes) > ListingAttributeLimits.MAX_PAYLOAD_BYTES) {
            issues += ListingAttributeIssue(null, "Listing attributes are too large.")
        }
        attributes.values.forEach { (key, value) ->
            val definition = ListingAttributeCatalog.definition(category, key)
            if (definition == null || ListingAttributeCatalog.isRetired(key)) {
                issues += ListingAttributeIssue(key, "$key does not apply to this category.")
            } else {
                issues += validateValue(definition, value)
                definition.applicability?.let { issues += validateApplicability(definition, it, attributes) }
            }
        }
        return issues
    }

    private fun validateApplicability(
        definition: ListingAttributeDefinition,
        applicability: ListingAttributeApplicability,
        attributes: ListingAttributes
    ): List<ListingAttributeIssue> {
        val controlling = (attributes[applicability.controllingKey] as? ListingAttributeValue.Choice)?.code
        return if (controlling in applicability.allowedCodes) {
            emptyList()
        } else {
            listOf(
                ListingAttributeIssue(definition.key, "${definition.label} does not apply to the selected device type.")
            )
        }
    }

    private fun validateValue(
        definition: ListingAttributeDefinition,
        value: ListingAttributeValue
    ): List<ListingAttributeIssue> {
        val message = when (val type = definition.type) {
            is ListingAttributeType.Text -> textProblem(definition, type, value)
            is ListingAttributeType.Choice -> choiceProblem(definition, type, value)
            is ListingAttributeType.Integer -> integerProblem(definition, type, value)
            is ListingAttributeType.Decimal -> decimalProblem(definition, type, value)
            is ListingAttributeType.Date -> dateProblem(definition, type, value)
        }
        return listOfNotNull(message?.let { ListingAttributeIssue(definition.key, it) })
    }

    private fun textProblem(
        definition: ListingAttributeDefinition,
        type: ListingAttributeType.Text,
        value: ListingAttributeValue
    ): String? {
        val text = value as? ListingAttributeValue.Text ?: return wrongType(definition)
        val limit = minOf(type.maxLength, ListingAttributeLimits.MAX_TEXT_LENGTH)
        val valid = text.value.length <= limit && ListingAttributeText.normalize(type.format, text.value) == text.value
        return if (valid) null else "Enter a valid ${definition.label.lowercase()}."
    }

    private fun choiceProblem(
        definition: ListingAttributeDefinition,
        type: ListingAttributeType.Choice,
        value: ListingAttributeValue
    ): String? {
        val choice = value as? ListingAttributeValue.Choice ?: return wrongType(definition)
        return if (type.options.any { it.code == choice.code }) {
            null
        } else {
            "Choose a valid ${definition.label.lowercase()}."
        }
    }

    private fun integerProblem(
        definition: ListingAttributeDefinition,
        type: ListingAttributeType.Integer,
        value: ListingAttributeValue
    ): String? {
        val number = value as? ListingAttributeValue.Integer ?: return wrongType(definition)
        return if (number.value in type.min..type.max) {
            null
        } else {
            "${definition.label} must be between ${type.min} and ${type.max}."
        }
    }

    private fun decimalProblem(
        definition: ListingAttributeDefinition,
        type: ListingAttributeType.Decimal,
        value: ListingAttributeValue
    ): String? {
        val number = value as? ListingAttributeValue.Decimal ?: return wrongType(definition)
        val inRange = number.value.scale == type.scale && number.value >= type.min && number.value <= type.max
        return if (inRange) null else "${definition.label} must be between ${type.min} and ${type.max}."
    }

    private fun dateProblem(
        definition: ListingAttributeDefinition,
        type: ListingAttributeType.Date,
        value: ListingAttributeValue
    ): String? {
        val date = value as? ListingAttributeValue.Date ?: return wrongType(definition)
        return if (date.value.year in type.minYear..type.maxYear) {
            null
        } else {
            "Choose a date between ${type.minYear} and ${type.maxYear}."
        }
    }

    private fun wrongType(definition: ListingAttributeDefinition) = "${definition.label} has the wrong kind of value."

    /** Conservative size estimate of the persisted JSON object; the database trigger measures the exact text. */
    private fun estimatedPayloadBytes(attributes: ListingAttributes): Int {
        val typed = attributes.values.entries.sumOf { (key, value) ->
            key.encodeToByteArray().size + valueJsonLength(value) + ENTRY_OVERHEAD
        }
        val raw = attributes.preserved.entries.sumOf { (key, json) ->
            key.encodeToByteArray().size + json.encodeToByteArray().size + ENTRY_OVERHEAD
        }
        return typed + raw + OBJECT_OVERHEAD
    }

    private fun valueJsonLength(value: ListingAttributeValue): Int = when (value) {
        is ListingAttributeValue.Text -> value.value.encodeToByteArray().size + value.value.count {
            it == '"' || it == '\\'
        } + QUOTES
        is ListingAttributeValue.Choice -> value.code.length + QUOTES
        is ListingAttributeValue.Integer -> value.value.toString().length
        is ListingAttributeValue.Decimal -> value.value.toPlainString().length
        is ListingAttributeValue.Date -> value.value.toString().length + QUOTES
    }

    private companion object {
        const val ENTRY_OVERHEAD = 6
        const val OBJECT_OVERHEAD = 2
        const val QUOTES = 2
    }
}
