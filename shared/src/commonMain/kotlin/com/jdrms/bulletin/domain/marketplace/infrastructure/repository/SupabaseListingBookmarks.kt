package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarks
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkWithListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** Persists bookmark membership; the database primary key prevents duplicate user/listing pairs. */
internal class SupabaseListingBookmarks internal constructor(
    private val fetch: suspend (String, Long) -> List<ListingBookmarkDto>,
    private val upsert: suspend (ListingBookmarkDto) -> Unit,
    private val delete: suspend (String, String) -> Unit,
    private val fetchPage: suspend (String, Long, Int) -> List<ListingBookmarkWithListingDto>
) {
    constructor(supabase: SupabaseClient) : this(
        fetch = { userId, offset ->
            supabase.from(TABLE).select(columns = Columns.raw("user_id,listing_id")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.ASCENDING)
                order("listing_id", Order.ASCENDING)
                range(offset, offset + PAGE_SIZE - 1)
            }.decodeList<ListingBookmarkDto>()
        },
        upsert = { bookmark ->
            supabase.from(TABLE).upsert(bookmark) {
                onConflict = "user_id,listing_id"
                ignoreDuplicates = true
            }
        },
        delete = { userId, listingId ->
            supabase.from(TABLE).delete {
                filter {
                    eq("user_id", userId)
                    eq("listing_id", listingId)
                }
            }
        },
        fetchPage = { userId, offset, pageSize ->
            supabase.from(TABLE).select(columns = BOOKMARKED_LISTING_COLUMNS) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                order("listing_id", Order.DESCENDING)
                range(offset, offset + pageSize)
            }.decodeList<ListingBookmarkWithListingDto>()
        }
    )

    suspend fun get(userId: String): ListingBookmarks {
        val rows = mutableListOf<ListingBookmarkDto>()
        var offset = 0L
        do {
            val page = fetch(userId, offset)
            rows += page
            offset += page.size
        } while (page.size == PAGE_SIZE.toInt())
        return ListingBookmarks.of(userId, rows.map { MarketplaceItemId("listing:${it.listingId}") })
    }

    suspend fun bookmark(userId: String, itemId: MarketplaceItemId) {
        upsert(ListingBookmarkDto(userId, itemId.value.removePrefix(LISTING_ID_PREFIX)))
    }

    suspend fun remove(userId: String, itemId: MarketplaceItemId) {
        delete(userId, itemId.value.removePrefix(LISTING_ID_PREFIX))
    }

    suspend fun getPage(
        userId: String,
        cursor: ListingBookmarksPageCursor?,
        pageSize: Int
    ): BookmarkedListingsPage {
        val offset = cursor?.offset ?: 0
        val rows = fetchPage(userId, offset.toLong(), pageSize)
        val pageRows = rows.take(pageSize)
        return BookmarkedListingsPage(
            listingIds = pageRows.map { row -> MarketplaceItemId("$LISTING_ID_PREFIX${row.listingId}") },
            listings = pageRows.mapNotNull { row ->
                row.listing?.let { listing -> SupabaseMarketplaceListingMapper.toListing(listing, null) }
            },
            nextCursor = if (rows.size > pageSize) {
                ListingBookmarksPageCursor(offset + pageRows.size)
            } else {
                null
            }
        )
    }

    companion object {
        const val TABLE = "listing_bookmarks"
        const val PAGE_SIZE = 500L
        const val LISTING_ID_PREFIX = "listing:"
        val BOOKMARKED_LISTING_COLUMNS = Columns.raw(
            "listing_id,listings(id,name,user_id,category,condition,price,description,created_at,profiles(full_name))"
        )
    }
}
