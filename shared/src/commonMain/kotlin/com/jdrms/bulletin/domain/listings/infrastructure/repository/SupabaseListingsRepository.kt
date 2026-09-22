package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabaseListingsRepository(
    private val supabase: SupabaseClient
) : ListingsRepository {

    override suspend fun createListing(listing: Listing): Result<Listing> {
        return runCatching {
            val sellerId = resolveSellerId(listing.sellerId)
            supabase.from(LISTINGS_TABLE).insert(SupabaseListingMapper.toInsertDto(listing, sellerId))
            listing.copy(sellerId = SellerId(sellerId))
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapListingsErrorMessage(it), it)) }
        )
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        return runCatching {
            val sellerId = resolveSellerId(listing.sellerId)
            supabase.from(LISTINGS_TABLE).update(SupabaseListingMapper.toInsertDto(listing, sellerId)) {
                filter { eq("id", listing.id.value) }
            }
            listing.copy(sellerId = SellerId(sellerId))
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapListingsErrorMessage(it), it)) }
        )
    }

    override suspend fun deleteListing(id: ListingId): Result<Unit> {
        return runCatching {
            supabase.from(LISTINGS_TABLE).delete {
                filter { eq("id", id.value) }
            }
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapListingsErrorMessage(it), it)) }
        )
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        val resolvedSellerId = resolveSellerIdOrNull(sellerId) ?: return emptyList()
        return runCatching {
            supabase.from(LISTINGS_TABLE).select {
                filter { eq("user_id", resolvedSellerId) }
            }.decodeList<SupabaseListingDto>().mapNotNull(SupabaseListingMapper::toDomain)
        }.getOrDefault(emptyList())
    }

    override suspend fun getAllListings(): List<Listing> {
        return runCatching {
            supabase.from(LISTINGS_TABLE).select()
                .decodeList<SupabaseListingDto>()
                .mapNotNull(SupabaseListingMapper::toDomain)
        }.getOrDefault(emptyList())
    }

    private suspend fun resolveSellerId(sellerId: SellerId): String {
        return resolveSellerIdOrNull(sellerId)
            ?: error("You must be logged in to manage listings.")
    }

    private suspend fun resolveSellerIdOrNull(sellerId: SellerId): String? {
        return if (sellerId.value == CURRENT_SELLER_ID) {
            supabase.auth.currentUserOrNull()?.id
        } else {
            sellerId.value.takeIf(String::isNotBlank)
        }
    }

    companion object {
        const val LISTINGS_TABLE = "listings"
        private const val CURRENT_SELLER_ID = "current_student"

        private val ERROR_RULES = listOf(
            listOf("could not find the table", "schema cache") to
                "Database table 'listings' not found. Please verify your Supabase schema setup.",
            listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
                "Unable to connect to server. Please check your internet connection.",
            listOf("jwt", "unauthorized", "invalid api key", "no api key") to
                "Unauthorized database request. Please check your Supabase API credentials.",
            listOf("row-level security", "rls") to
                "Database permission denied. Please check your Supabase RLS policies."
        )

        fun mapListingsErrorMessage(throwable: Throwable): String {
            val message = throwable.message ?: return "An unexpected listings error occurred."
            val lower = message.lowercase()
            ERROR_RULES.firstOrNull { (patterns, _) -> patterns.any(lower::contains) }?.let {
                return it.second
            }
            return message.lines().firstOrNull { it.isNotBlank() }?.trim()
                ?: "Listings request failed."
        }
    }
}
