package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal object MarketplaceInterestFilterJsonMapper {
    fun encode(filters: MarketplaceAttributeFilters): JsonObject = buildJsonObject {
        filters.allPredicates.forEach { (key, predicate) -> put(key, encodePredicate(predicate)) }
    }

    fun decode(json: JsonObject): MarketplaceAttributeFilters = MarketplaceAttributeFilters(
        predicates = json.mapValues { (_, element) -> decodePredicate(element.jsonObject) }
    )

    private fun encodePredicate(predicate: MarketplaceAttributePredicate): JsonObject = buildJsonObject {
        when (predicate) {
            is MarketplaceAttributePredicate.ExactText -> {
                put(TYPE, EXACT_TEXT)
                put(VALUE, predicate.value)
                put(USES_MATCH_KEY, predicate.usesMatchKey)
            }
            is MarketplaceAttributePredicate.Choice -> {
                put(TYPE, CHOICE)
                put(CODE, predicate.code)
            }
            is MarketplaceAttributePredicate.ChoiceOptions -> {
                put(TYPE, CHOICE_OPTIONS)
                put(VALUES, JsonArray(predicate.codes.sorted().map(::JsonPrimitive)))
            }
            is MarketplaceAttributePredicate.IntegerOptions -> {
                put(TYPE, INTEGER_OPTIONS)
                val ranges = predicate.ranges.sortedWith(compareBy({ it.min }, { it.max }))
                val values = ranges.map { encodePredicate(MarketplaceAttributePredicate.IntegerRange(it)) }
                put(VALUES, JsonArray(values))
            }
            is MarketplaceAttributePredicate.IntegerRange -> {
                put(TYPE, INTEGER_RANGE)
                predicate.range.min?.let { put(MIN, it) }
                predicate.range.max?.let { put(MAX, it) }
            }
            is MarketplaceAttributePredicate.DecimalRange -> {
                put(TYPE, DECIMAL_RANGE)
                predicate.min?.let { put(MIN, it) }
                predicate.max?.let { put(MAX, it) }
            }
            is MarketplaceAttributePredicate.DateRange -> {
                put(TYPE, DATE_RANGE)
                predicate.minIso?.let { put(MIN, it) }
                predicate.maxIso?.let { put(MAX, it) }
            }
        }
    }

    private fun decodePredicate(json: JsonObject): MarketplaceAttributePredicate = when (json.string(TYPE)) {
        EXACT_TEXT -> MarketplaceAttributePredicate.ExactText(
            value = json.requiredString(VALUE),
            usesMatchKey = json[USES_MATCH_KEY]?.jsonPrimitive?.booleanOrNull ?: false
        )
        CHOICE -> MarketplaceAttributePredicate.Choice(json.requiredString(CODE))
        CHOICE_OPTIONS -> MarketplaceAttributePredicate.ChoiceOptions(
            json.getValue(VALUES).jsonArray.map { it.jsonPrimitive.content }.toSet()
        )
        INTEGER_OPTIONS -> MarketplaceAttributePredicate.IntegerOptions(
            json.getValue(VALUES).jsonArray.map {
                MarketplaceCapacityRange(it.jsonObject.long(MIN), it.jsonObject.long(MAX))
            }.toSet()
        )
        INTEGER_RANGE -> MarketplaceAttributePredicate.IntegerRange(
            MarketplaceCapacityRange(json.long(MIN), json.long(MAX))
        )
        DECIMAL_RANGE -> MarketplaceAttributePredicate.DecimalRange(json.double(MIN), json.double(MAX))
        DATE_RANGE -> MarketplaceAttributePredicate.DateRange(json.string(MIN), json.string(MAX))
        else -> error("Unknown marketplace interest filter type.")
    }

    private fun JsonObject.requiredString(key: String): String =
        requireNotNull(string(key)) { "Marketplace interest filter is missing $key." }

    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject.long(key: String): Long? = get(key)?.jsonPrimitive?.longOrNull
    private fun JsonObject.double(key: String): Double? = get(key)?.jsonPrimitive?.doubleOrNull

    private const val VALUES = "values"
    private const val CHOICE_OPTIONS = "choice_options"
    private const val INTEGER_OPTIONS = "integer_options"
    private const val TYPE = "type"
    private const val VALUE = "value"
    private const val CODE = "code"
    private const val MIN = "min"
    private const val MAX = "max"
    private const val USES_MATCH_KEY = "uses_match_key"
    private const val EXACT_TEXT = "exact_text"
    private const val CHOICE = "choice"
    private const val INTEGER_RANGE = "integer_range"
    private const val DECIMAL_RANGE = "decimal_range"
    private const val DATE_RANGE = "date_range"
}
