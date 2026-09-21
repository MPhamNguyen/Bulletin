package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SupabaseMarketplaceListingSource private constructor(
    private val listingTable: MarketplaceListingTable
) : MarketplaceListingSource {
    constructor(supabase: SupabaseClient) : this(PostgrestMarketplaceListingTable(supabase))

    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> {
        return listingTable.getListings().mapNotNull(SupabaseMarketplaceListingMapper::toSnapshot)
    }

    internal constructor(getListings: suspend () -> List<SupabaseMarketplaceListingDto>) :
        this(MarketplaceListingTable(getListings))
}

private fun interface MarketplaceListingTable {
    suspend fun getListings(): List<SupabaseMarketplaceListingDto>
}

private class PostgrestMarketplaceListingTable(
    private val supabase: SupabaseClient
) : MarketplaceListingTable {
    override suspend fun getListings(): List<SupabaseMarketplaceListingDto> {
        return supabase.from(LISTINGS_TABLE).select().decodeList()
    }

    private companion object {
        const val LISTINGS_TABLE = "listings"
    }
}
