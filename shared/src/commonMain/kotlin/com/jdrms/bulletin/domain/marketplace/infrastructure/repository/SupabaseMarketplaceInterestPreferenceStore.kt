package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceException
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceInterestDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceInterestMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseMarketplaceInterestPreferenceStore internal constructor(
    private val authenticatedUserId: () -> String?,
    private val fetch: suspend (String) -> List<MarketplaceInterestDto>,
    private val replace: suspend (List<MarketplaceInterestDto>) -> Unit
) : MarketplaceInterestPreferenceStore {
    constructor(supabase: SupabaseClient) : this(
        authenticatedUserId = { supabase.auth.currentUserOrNull()?.id },
        fetch = { ownerId ->
            supabase.from(TABLE).select(columns = INTEREST_COLUMNS) {
                filter { eq("user_id", ownerId) }
            }.decodeList()
        },
        replace = { interests ->
            supabase.postgrest.rpc(
                FUNCTION,
                parameters = buildJsonObject {
                    put(
                        "p_interests",
                        buildJsonArray {
                            interests.sortedBy { it.category }.forEach {
                                add(Json.encodeToJsonElement(MarketplaceInterestDto.serializer(), it))
                            }
                        }
                    )
                }
            )
        }
    )

    override suspend fun get(ownerId: MarketplaceInterestOwnerId): Set<MarketplaceInterest> {
        requireAuthenticatedOwner(ownerId)
        return translateFailure("Unable to load marketplace interests.") {
            fetch(ownerId.value).mapTo(mutableSetOf(), MarketplaceInterestMapper::toDomain)
        }
    }

    override suspend fun set(
        ownerId: MarketplaceInterestOwnerId,
        interests: Set<MarketplaceInterest>
    ) {
        requireAuthenticatedOwner(ownerId)
        translateFailure("Unable to save marketplace interests.") {
            replace(interests.map(MarketplaceInterestMapper::toDto))
        }
    }

    private suspend fun <T> translateFailure(message: String, operation: suspend () -> T): T =
        runCatching { operation() }.getOrElse { cause ->
            when (cause) {
                is CancellationException -> throw cause
                is Error -> throw cause
                else -> throw MarketplaceInterestPreferenceException(message, cause)
            }
        }

    private fun requireAuthenticatedOwner(ownerId: MarketplaceInterestOwnerId) {
        check(authenticatedUserId() == ownerId.value) {
            "Marketplace interests require the authenticated owner."
        }
    }

    companion object {
        const val TABLE = "marketplace_interests"
        const val FUNCTION = "replace_marketplace_interests"
        val INTEREST_COLUMNS = Columns.list(
            "category",
            "condition",
            "minimum_price_cents",
            "maximum_price_cents",
            "attribute_filters"
        )
    }
}
