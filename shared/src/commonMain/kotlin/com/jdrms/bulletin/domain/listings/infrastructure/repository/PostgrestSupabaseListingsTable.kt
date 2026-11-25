package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingEditRequest
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

internal class PostgrestSupabaseListingsTable(
    private val supabase: SupabaseClient
) : SupabaseListingsTable {
    override suspend fun findById(id: String): SupabaseListingDto? {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select(columns = LISTING_COLUMNS) {
            filter { eq("id", id) }
        }.decodeSingleOrNull()
    }

    override suspend fun insert(listing: SupabaseListingInsertDto) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).insert(listing)
    }

    override suspend fun update(listing: SupabaseListingWriteDto, intent: ListingUpdateIntent) {
        if (intent == ListingUpdateIntent.EDIT) {
            supabase.postgrest.rpc("edit_listing", SupabaseListingEditRequest(listing))
            return
        }
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).update(listing) {
            filter { eq("id", listing.id) }
        }
    }

    override suspend fun delete(id: String, sellerId: String) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).delete {
            filter {
                eq("id", id)
                eq("user_id", sellerId)
            }
        }
    }

    override suspend fun getListings(sellerId: String?): List<SupabaseListingDto> {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select(columns = LISTING_COLUMNS) {
            sellerId?.let { filter { eq("user_id", it) } }
        }.decodeList()
    }

    private companion object {
        val LISTING_COLUMNS = Columns.raw(
            """
            *,
            seller_profile:profiles!listings_user_id_fkey ( full_name ),
            listing_images(object_path,position,crop_scale,crop_offset_x,crop_offset_y)
            """.trimIndent()
        )
    }
}
