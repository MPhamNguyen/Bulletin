package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.query.filter.TextSearchType

class SupabaseMarketplaceListingSource private constructor(
    private val listingTable: MarketplaceListingTable
) : MarketplaceListingSource {
    constructor(supabase: SupabaseClient) : this(PostgrestMarketplaceListingTable(supabase))

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        val rows = listingTable.getListings(request)
        val mappedRows = rows.mapNotNull { row ->
            SupabaseMarketplaceListingMapper.toSnapshot(row)?.let { snapshot -> row to snapshot }
        }
        val pageRows = mappedRows.take(request.pageSize)
        val cursorRow = when {
            pageRows.size == request.pageSize -> pageRows.last().first
            rows.size >= request.pageSize -> rows.last()
            else -> null
        }
        return MarketplaceListingPage(
            listings = pageRows.map { it.second },
            nextCursor = cursorRow?.let(SupabaseMarketplaceListingMapper::toPageCursor)
        )
    }

    internal constructor(getListings: suspend (MarketplacePageRequest) -> List<SupabaseMarketplaceListingDto>) :
        this(MarketplaceListingTable(getListings))
}

private fun interface MarketplaceListingTable {
    suspend fun getListings(request: MarketplacePageRequest): List<SupabaseMarketplaceListingDto>
}

private class PostgrestMarketplaceListingTable(
    private val supabase: SupabaseClient
) : MarketplaceListingTable {
    override suspend fun getListings(request: MarketplacePageRequest): List<SupabaseMarketplaceListingDto> {
        return supabase.from(LISTINGS_TABLE).select {
            filter {
                applyMarketplaceSearch(request)

                request.cursor?.let { cursor ->
                    val cursorTimestamp = cursor.createdAt.toString()
                    or {
                        lt("created_at", cursorTimestamp)
                        if (cursor.itemId.startsWith(LISTING_ID_PREFIX)) {
                            and {
                                eq("created_at", cursorTimestamp)
                                lt("id", cursor.itemId.removePrefix(LISTING_ID_PREFIX))
                            }
                        }
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit((request.pageSize + 1).toLong())
        }.decodeList()
    }

    private companion object {
        const val LISTINGS_TABLE = "listings"
        const val LISTING_ID_PREFIX = "listing:"
    }
}

internal fun PostgrestFilterBuilder.applyMarketplaceSearch(request: MarketplacePageRequest) {
    request.category?.let { category ->
        ilike("category", category.name)
    }
    request.query.toSafePrefixSearchQuery()?.let { query ->
        textSearch("name", query, TextSearchType.NONE)
    }
}

internal fun String.toSafePrefixSearchQuery(): String? {
    return SEARCH_TERM_REGEX.findAll(lowercase())
        .map { match -> "${match.value}:*" }
        .joinToString(" & ")
        .takeIf(String::isNotEmpty)
}

private val SEARCH_TERM_REGEX = Regex("[\\p{L}\\p{N}]+")
