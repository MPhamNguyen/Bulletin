package com.jdrms.bulletin.domain.listings.infrastructure.mapper

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeValidationPolicy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/** Outcome of reading the `attributes` column. [Malformed] means the payload is not a JSON object at all. */
sealed interface ListingAttributesDecoding {
    data class Decoded(val attributes: ListingAttributes) : ListingAttributesDecoding
    data class Malformed(val reason: String) : ListingAttributesDecoding
}

/**
 * Maps the JSONB `attributes` column to [ListingAttributes] and back.
 *
 * Decoding never discards data. Entries that are unknown, retired, not applicable to the category, the wrong
 * JSON type, or out of bounds are kept verbatim in [ListingAttributes.preserved] so a later write returns them to
 * storage untouched. A payload that is not an object is reported as [ListingAttributesDecoding.Malformed] rather
 * than being turned into empty attributes that a later write would persist over the original.
 */
class ListingAttributesJsonCodec(
    private val policy: ListingAttributeValidationPolicy = ListingAttributeValidationPolicy()
) {
    fun decode(element: JsonElement?, category: ListingCategory): ListingAttributesDecoding {
        if (element == null) return ListingAttributesDecoding.Decoded(ListingAttributes.EMPTY)
        val obj = element as? JsonObject
            ?: return ListingAttributesDecoding.Malformed("attributes must be a JSON object")
        val values = mutableMapOf<String, ListingAttributeValue>()
        val preserved = mutableMapOf<String, String>()
        obj.forEach { (key, json) ->
            val definition = ListingAttributeCatalog.definition(category, key)?.takeUnless {
                ListingAttributeCatalog.isRetired(key)
            }
            val value = definition?.let { decodeValue(it, json) }
            if (value == null) preserved[key] = json.toString() else values[key] = value
        }
        quarantineInvalid(category, values, preserved, obj)
        return ListingAttributesDecoding.Decoded(ListingAttributes(values, preserved))
    }

    private fun quarantineInvalid(
        category: ListingCategory,
        values: MutableMap<String, ListingAttributeValue>,
        preserved: MutableMap<String, String>,
        source: JsonObject
    ) {
        policy.validate(category, ListingAttributes(values)).mapNotNull { it.key }.toSet().forEach { key ->
            values.remove(key)
            source[key]?.let { preserved[key] = it.toString() }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun encode(attributes: ListingAttributes): JsonObject {
        val entries = mutableMapOf<String, JsonElement>()
        attributes.preserved.forEach { (key, raw) -> entries[key] = Json.parseToJsonElement(raw) }
        attributes.values.forEach { (key, value) ->
            entries[key] = when (value) {
                is ListingAttributeValue.Text -> JsonPrimitive(value.value)
                is ListingAttributeValue.Choice -> JsonPrimitive(value.code)
                is ListingAttributeValue.Integer -> JsonPrimitive(value.value)
                is ListingAttributeValue.Decimal -> JsonUnquotedLiteral(value.value.toPlainString())
                is ListingAttributeValue.Date -> JsonPrimitive(value.value.toString())
            }
        }
        return JsonObject(entries)
    }

    private fun decodeValue(definition: ListingAttributeDefinition, json: JsonElement): ListingAttributeValue? {
        val primitive = json as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        return when (val type = definition.type) {
            is ListingAttributeType.Text -> primitive.takeIf { it.isString }?.content
                ?.let(ListingAttributeValue::Text)
            is ListingAttributeType.Choice -> primitive.takeIf { it.isString }?.content
                ?.let(ListingAttributeValue::Choice)
            is ListingAttributeType.Integer -> primitive.takeUnless { it.isString }?.content
                ?.takeIf { WHOLE_NUMBER.matches(it) }
                ?.let { ListingAttributeValue.Integer(it.toLong()) }
            is ListingAttributeType.Decimal -> primitive.takeUnless { it.isString }?.content
                ?.let { ListingDecimal.parse(it, type.scale) }
                ?.let(ListingAttributeValue::Decimal)
            is ListingAttributeType.Date -> primitive.takeIf { it.isString }?.content
                ?.let(ListingAttributeDate::parse)
                ?.let(ListingAttributeValue::Date)
        }
    }

    private companion object {
        val WHOLE_NUMBER = Regex("^\\d{1,18}$")
    }
}
