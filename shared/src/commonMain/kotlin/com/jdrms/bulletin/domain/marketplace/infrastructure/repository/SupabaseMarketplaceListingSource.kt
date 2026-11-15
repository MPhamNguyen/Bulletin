package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
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
            nextCursor = cursorRow?.let { SupabaseMarketplaceListingMapper.toPageCursor(it, request.sortOrder) }
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

                applyMarketplaceCursor(request)
            }
            when (request.sortOrder) {
                MarketplaceSortOrder.NEWEST -> order("created_at", Order.DESCENDING)
                MarketplaceSortOrder.PRICE_LOW_TO_HIGH -> order("price", Order.ASCENDING)
                MarketplaceSortOrder.PRICE_HIGH_TO_LOW -> order("price", Order.DESCENDING)
            }
            order(
                "id",
                if (request.sortOrder == MarketplaceSortOrder.PRICE_LOW_TO_HIGH) {
                    Order.ASCENDING
                } else {
                    Order.DESCENDING
                }
            )
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

private fun PostgrestFilterBuilder.applyMarketplaceCursor(request: MarketplacePageRequest) {
    val cursor = request.cursor ?: return
    val rawId = cursor.itemId.removePrefix("listing:")
    val price = cursor.priceAmount
    if (request.sortOrder != MarketplaceSortOrder.NEWEST && price != null) {
        or {
            if (request.sortOrder == MarketplaceSortOrder.PRICE_LOW_TO_HIGH) {
                gt("price", price)
            } else {
                lt("price", price)
            }
            and {
                eq("price", price)
                if (request.sortOrder == MarketplaceSortOrder.PRICE_LOW_TO_HIGH) {
                    gt("id", rawId)
                } else {
                    lt("id", rawId)
                }
            }
        }
        return
    }
    val timestamp = cursor.createdAt.toString()
    or {
        lt("created_at", timestamp)
        and {
            eq("created_at", timestamp)
            lt("id", rawId)
        }
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
        if (request.category == null) neq("category", MarketplaceCategory.HOUSING.name)
        ilike("condition", condition.name)
    }
    request.priceRange.minimumAmount?.let { minimum ->
        gte("price", minimum)
    }
    request.priceRange.maximumAmount?.let { maximum ->
        lte("price", maximum)
    }
    request.query.toSafePrefixSearchQuery()?.let { query ->
        if (request.category.supportsBrandSearch()) {
            or {
                textSearch("name", query, TextSearchType.NONE)
                eq(
                    MarketplaceAttributeColumns.BRAND_KEY,
                    MarketplaceAttributeMatching.brandKey(request.query)
                )
            }
        } else {
            textSearch("name", query, TextSearchType.NONE)
        }
    }
    applyAttributeFilters(request.attributeFilters, request.category)
}

private fun MarketplaceCategory?.supportsBrandSearch(): Boolean =
    this == null || this == MarketplaceCategory.CLOTHING || this == MarketplaceCategory.ELECTRONICS

internal fun String.toSafePrefixSearchQuery(): String? {
    return SEARCH_TERM_REGEX.findAll(lowercase())
        .map { match -> "${match.value}:*" }
        .joinToString(" & ")
        .takeIf(String::isNotEmpty)
}

private val SEARCH_TERM_REGEX = Regex("[\\p{L}\\p{N}]+")
