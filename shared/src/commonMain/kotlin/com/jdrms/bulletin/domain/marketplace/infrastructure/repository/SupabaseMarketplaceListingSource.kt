package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import io.github.jan.supabase.storage.storage

class SupabaseMarketplaceListingSource private constructor(
    private val listingTable: MarketplaceListingTable,
    private val readCache: MarketplaceReadCache,
    private val photoUrl: (String) -> String
) : MarketplaceListingSource {
    constructor(supabase: SupabaseClient) : this(
        PostgrestMarketplaceListingTable(supabase),
        MarketplaceReadCache(),
        { path -> supabase.storage.from(MARKETPLACE_IMAGES_BUCKET).publicUrl(path) }
    )

    internal constructor(supabase: SupabaseClient, readCache: MarketplaceReadCache) :
        this(
            PostgrestMarketplaceListingTable(supabase),
            readCache,
            { path -> supabase.storage.from(MARKETPLACE_IMAGES_BUCKET).publicUrl(path) }
        )

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return readCache.page(request) { loadPage(request) }
    }

    private suspend fun loadPage(request: MarketplacePageRequest): MarketplaceListingPage {
        val rows = listingTable.getListings(request)
        val mappedRows = rows.mapNotNull { row ->
            SupabaseMarketplaceListingMapper.toSnapshot(row, photoUrl)?.let { snapshot -> row to snapshot }
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
        this(MarketplaceListingTable(getListings), MarketplaceReadCache(), { it })

    internal constructor(
        fetch: suspend (MarketplacePageRequest) -> List<SupabaseMarketplaceListingDto>,
        imageUrl: (String) -> String
    ) : this(MarketplaceListingTable(fetch), MarketplaceReadCache(), imageUrl)
}

private fun interface MarketplaceListingTable {
    suspend fun getListings(request: MarketplacePageRequest): List<SupabaseMarketplaceListingDto>
}

private class PostgrestMarketplaceListingTable(
    private val supabase: SupabaseClient
) : MarketplaceListingTable {
    override suspend fun getListings(request: MarketplacePageRequest): List<SupabaseMarketplaceListingDto> {
        return supabase.from(LISTINGS_TABLE).select(columns = LISTING_COLUMNS) {
            filter {
                applyMarketplaceSearch(request)
                eq("is_sold", false)

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
        val LISTING_COLUMNS = Columns.raw(
            "*, seller_profile:profiles!listings_user_id_fkey ( full_name ), " +
                "listing_images ( object_path, position, crop_scale, crop_offset_x, crop_offset_y )"
        )
    }
}

private const val MARKETPLACE_IMAGES_BUCKET = "listing_imgs"

internal fun PostgrestFilterBuilder.applyMarketplaceSearch(request: MarketplacePageRequest) {
    request.listingId?.let { listingId ->
        eq("id", listingId.removePrefix("listing:"))
    }
    request.viewerId?.let { viewerId ->
        neq("user_id", viewerId)
    }
    request.sellerId?.let { sellerId ->
        eq("user_id", sellerId)
    }
    request.category?.let { category ->
        ilike("category", category.name)
    }
    request.condition?.let { condition ->
        ilike("condition", condition.name)
    }
    request.query.toSafePrefixSearchQuery()?.let { query ->
        textSearch("name", query, TextSearchType.NONE)
    }
    applyAttributeFilters(request.attributeFilters)
}

internal fun String.toSafePrefixSearchQuery(): String? {
    return SEARCH_TERM_REGEX.findAll(lowercase())
        .map { match -> "${match.value}:*" }
        .joinToString(" & ")
        .takeIf(String::isNotEmpty)
}

private val SEARCH_TERM_REGEX = Regex("[\\p{L}\\p{N}]+")
