package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeApplicability
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.model.ListingTextFormat

/** Result of turning form text into typed attributes: either [attributes] or per-key [errors], never both. */
data class ListingAttributeInputResult(
    val attributes: ListingAttributes?,
    val errors: Map<String, String>
)

/**
 * Parses raw form input (key to text) for [ListingCategory]. Blank optional fields are omitted. Fields whose
 * applicability is not met are not visible, so their text is ignored rather than submitted or rejected.
 */
class ListingAttributeInputParser(
    private val policy: ListingAttributeValidationPolicy = ListingAttributeValidationPolicy()
) {
    fun parse(
        category: ListingCategory,
        inputs: Map<String, String>,
        preserved: Map<String, String> = emptyMap()
    ): ListingAttributeInputResult {
        val errors = mutableMapOf<String, String>()
        val values = mutableMapOf<String, ListingAttributeValue>()
        val applicable = applicableDefinitions(category, inputs)
        applicable.forEach { definition ->
            val raw = inputs[definition.key].orEmpty()
            if (raw.isBlank()) return@forEach
            when (val parsed = parseValue(definition, raw)) {
                is Parsed.Value -> values[definition.key] = parsed.value
                is Parsed.Failure -> errors[definition.key] = parsed.message
            }
        }
        val attributes = ListingAttributes(values, preserved - values.keys)
        policy.validate(category, attributes).forEach { issue ->
            errors.putIfAbsent(issue.key ?: GENERAL_ERROR_KEY, issue.message)
        }
        return if (errors.isEmpty()) {
            ListingAttributeInputResult(attributes, emptyMap())
        } else {
            ListingAttributeInputResult(null, errors)
        }
    }

    /** Definitions that are visible and submitted given the current input, in catalog order. */
    fun applicableDefinitions(
        category: ListingCategory,
        inputs: Map<String, String>
    ): List<ListingAttributeDefinition> =
        ListingAttributeCatalog.definitionsFor(category).filter { definition ->
            definition.applicability?.isSatisfiedBy(inputs) ?: true
        }

    private fun parseValue(definition: ListingAttributeDefinition, raw: String): Parsed =
        when (val type = definition.type) {
            is ListingAttributeType.Text -> ListingAttributeText.normalize(type.format, raw)
                ?.let { Parsed.Value(ListingAttributeValue.Text(it)) }
                ?: Parsed.Failure(textMessage(definition))
            is ListingAttributeType.Choice -> raw.trim().takeIf { code -> type.options.any { it.code == code } }
                ?.let { Parsed.Value(ListingAttributeValue.Choice(it)) }
                ?: Parsed.Failure("Choose a valid ${definition.label.lowercase()}.")
            is ListingAttributeType.Integer -> parseInteger(definition, raw)
            is ListingAttributeType.Decimal -> ListingDecimal.parse(raw, type.scale)
                ?.let { Parsed.Value(ListingAttributeValue.Decimal(it)) }
                ?: Parsed.Failure("${definition.label} must be a number with at most ${type.scale} decimal places.")
            is ListingAttributeType.Date -> ListingAttributeDate.parse(raw)
                ?.let { Parsed.Value(ListingAttributeValue.Date(it)) }
                ?: Parsed.Failure(dateMessage(raw))
        }

    private fun dateMessage(raw: String): String =
        if (ISO_SHAPE.matches(raw.trim())) {
            "That date doesn't exist. Check the month and day."
        } else {
            "Use the date format YYYY-MM-DD."
        }

    private fun parseInteger(definition: ListingAttributeDefinition, raw: String): Parsed =
        raw.trim().takeIf { WHOLE_NUMBER.matches(it) }
            ?.let { Parsed.Value(ListingAttributeValue.Integer(it.toLong())) }
            ?: Parsed.Failure("${definition.label} must be a whole number.")

    private fun textMessage(definition: ListingAttributeDefinition): String = when (
        (definition.type as ListingAttributeType.Text).format
    ) {
        ListingTextFormat.ISBN -> "Enter a valid ISBN-10 or ISBN-13."
        ListingTextFormat.COURSE_CODE ->
            "Use a course code like CECS 328."
        ListingTextFormat.CLOTHING_SIZE ->
            "Use letters, numbers, spaces, or . / - (up to 12 characters)."
        ListingTextFormat.BRAND -> "Enter a valid brand name."
    }

    private sealed interface Parsed {
        data class Value(val value: ListingAttributeValue) : Parsed
        data class Failure(val message: String) : Parsed
    }

    companion object {
        /** Key under which attribute-set-wide problems (count, size) are reported. */
        const val GENERAL_ERROR_KEY = "_attributes"
        private val WHOLE_NUMBER = Regex("^\\d{1,9}$")
        private val ISO_SHAPE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}

fun ListingAttributeApplicability.isSatisfiedBy(inputs: Map<String, String>): Boolean =
    inputs[controllingKey]?.trim() in allowedCodes
