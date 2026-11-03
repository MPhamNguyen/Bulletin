package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceAttributeReadPolicy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads the `attributes` JSONB column for buyers. Marketplace never writes it, so anything it cannot interpret
 * (unknown or retired keys, wrong JSON types, unknown choice codes, a non-object payload) is simply not shown;
 * the stored data is untouched and the listings context remains responsible for preserving it.
 */
object MarketplaceAttributesJsonDecoder {
    private const val MAX_TEXT_LENGTH = 60
    private val WHOLE_NUMBER = Regex("^\\d{1,18}$")
    private val DECIMAL = Regex("^\\d{1,12}(\\.\\d{1,4})?$")
    private val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun decode(element: JsonElement?, category: MarketplaceCategory): MarketplaceAttributes {
        val obj = element as? JsonObject ?: return MarketplaceAttributes.EMPTY
        val values = MarketplaceAttributeCatalog.specsFor(category).mapNotNull { spec ->
            (obj[spec.key] as? JsonPrimitive)?.takeUnless { it is JsonNull }
                ?.let { primitive -> decodeValue(spec, primitive)?.let { spec.key to it } }
        }.toMap()
        return MarketplaceAttributeReadPolicy.recognized(MarketplaceAttributes(values))
    }

    private fun decodeValue(spec: MarketplaceAttributeSpec, primitive: JsonPrimitive): MarketplaceAttributeValue? {
        val content = primitive.content
        return when (spec.kind) {
            MarketplaceAttributeKind.TEXT ->
                content.takeIf { primitive.isString && it.isNotBlank() && it.length <= MAX_TEXT_LENGTH }
                    ?.let(MarketplaceAttributeValue::Text)
            MarketplaceAttributeKind.CHOICE ->
                content.takeIf { primitive.isString && it in spec.choices }?.let(MarketplaceAttributeValue::Choice)
            MarketplaceAttributeKind.INTEGER ->
                content.takeIf { !primitive.isString && WHOLE_NUMBER.matches(it) }
                    ?.let { MarketplaceAttributeValue.Integer(it.toLong()) }
            MarketplaceAttributeKind.DECIMAL ->
                content.takeIf { !primitive.isString && DECIMAL.matches(it) }?.let(MarketplaceAttributeValue::Decimal)
            MarketplaceAttributeKind.DATE ->
                content.takeIf { primitive.isString && ISO_DATE.matches(it) }?.let(MarketplaceAttributeValue::Date)
        }
    }
}
