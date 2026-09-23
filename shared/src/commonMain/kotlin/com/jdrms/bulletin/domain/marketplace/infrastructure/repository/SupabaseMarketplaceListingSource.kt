package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SupabaseMarketplaceListingSource private constructor(
    private val listingTable: MarketplaceListingTable
) : MarketplaceListingSource {
    constructor(supabase: SupabaseClient) : this(PostgrestMarketplaceListingTable(supabase))

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        val rows = listingTable.getListings(request)
        val consumedRows = rows.take(request.pageSize)
        val mappedPage = consumedRows
            .mapNotNull(SupabaseMarketplaceListingMapper::toSnapshot)
            .pageFor(request)
        val nextCursor = if (rows.size >= request.pageSize) {
            consumedRows.lastOrNull()?.let(SupabaseMarketplaceListingMapper::toPageCursor)
        } else {
            null
        }
        return mappedPage.copy(nextCursor = nextCursor)
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
                request.category?.let { category ->
                    ilike("category", category.name)
                }
                request.query.trim().lowercase().split(Regex("\\s+"))
                    .filter(String::isNotEmpty)
                    .forEach { keyword ->
                        or {
                            ilike("name", "%$keyword%")
                            ilike("category", "%$keyword%")
                        }
                    }

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
