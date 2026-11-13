package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingIdDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

internal class SupabaseMarketplaceListingIdSnapshotSource(
    private val supabase: SupabaseClient
) : MarketplaceListingIdSnapshotSource {
    override suspend fun latestIds(): Set<MarketplaceItemId> =
        supabase.from(LISTINGS_TABLE).select(columns = Columns.raw("id")) {
            order("created_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit(SNAPSHOT_SIZE)
        }.decodeList<MarketplaceListingIdDto>()
            .mapTo(linkedSetOf()) { MarketplaceItemId(it.id) }
}

private const val LISTINGS_TABLE = "listings"
private const val SNAPSHOT_SIZE = 100L
