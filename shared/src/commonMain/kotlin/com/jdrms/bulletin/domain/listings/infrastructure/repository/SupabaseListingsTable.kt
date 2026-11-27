package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto

internal interface SupabaseListingsTable {
    suspend fun findById(id: String): SupabaseListingDto?
    suspend fun insert(listing: SupabaseListingInsertDto)
    suspend fun update(listing: SupabaseListingWriteDto, intent: ListingUpdateIntent)
    suspend fun delete(id: String, sellerId: String)
    suspend fun getListings(sellerId: String?): List<SupabaseListingDto>
}
