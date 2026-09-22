package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ReviewScoreDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceMapper
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class SupabaseMarketplaceRepository(
    private val supabase: SupabaseClient,
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy(),
    private val fallbackRepository: InMemoryMarketplaceRepository = InMemoryMarketplaceRepository()
) : MarketplaceRepository {

    private val savedItemIdsByUser = mutableMapOf<String, MutableSet<MarketplaceItemId>>()

    override suspend fun getCatalog(): List<MarketplaceItem> {
        val timedResult = withTimeoutOrNull(TIMEOUT_MILLIS) {
            runCatching {
                val dtos = supabase.from(LISTINGS_TABLE).select().decodeList<MarketplaceItemDto>()
                if (dtos.isEmpty()) {
                    fallbackRepository.getCatalog()
                } else {
                    dtos.map { MarketplaceMapper.toDomain(it) }
                }
            }.getOrElse { error ->
                error.rethrowIfCancellation()
                fallbackRepository.getCatalog()
            }
        }
        return timedResult ?: fallbackRepository.getCatalog()
    }

    override suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        val items = getCatalog()
        return searchPolicy.filterItems(items, query, category)
    }

    override suspend fun getItem(id: MarketplaceItemId): MarketplaceItem? {
        val databaseListingId = normalizeListingId(id.value)
        val timedResult = withTimeoutOrNull(TIMEOUT_MILLIS) {
            runCatching {
                val dto = supabase.from(LISTINGS_TABLE).select {
                    filter {
                        eq("id", databaseListingId)
                    }
                }.decodeSingleOrNull<MarketplaceItemDto>()
                dto?.let { MarketplaceMapper.toDomain(it) }
            }.getOrElse { error ->
                error.rethrowIfCancellation()
                null
            }
        }
        return timedResult ?: fallbackRepository.getItem(id)
    }

    override suspend fun viewListing(listingID: String): Result<Listing> {
        val databaseListingId = normalizeListingId(listingID)
        val timedResult = withTimeoutOrNull(TIMEOUT_MILLIS) {
            runCatching {
                val listingDto = supabase.from(LISTINGS_TABLE).select {
                    filter {
                        eq("id", databaseListingId)
                    }
                }.decodeSingleOrNull<SupabaseMarketplaceListingDto>()

                if (listingDto != null) {
                    val reputationScore = listingDto.userId?.let { sellerId ->
                        runCatching {
                            val reviews = supabase.from(REVIEWS_TABLE).select {
                                filter {
                                    eq("reviewee_id", sellerId)
                                }
                            }.decodeList<ReviewScoreDto>()

                            reviews.takeIf(List<ReviewScoreDto>::isNotEmpty)
                                ?.map { it.score }
                                ?.average()
                        }.getOrElse { error ->
                            error.rethrowIfCancellation()
                            null
                        }
                    }

                    SupabaseMarketplaceListingMapper.toListing(
                        dto = listingDto,
                        reputationScore = reputationScore
                    ) ?: error("Supabase listing is missing required fields.")
                } else {
                    null
                }
            }.fold(
                onSuccess = { listing ->
                    if (listing != null) {
                        Result.Success(listing)
                    } else {
                        // Check fallback repository if not found on server
                        fallbackRepository.viewListing(databaseListingId)
                    }
                },
                onFailure = { error ->
                    error.rethrowIfCancellation()
                    val fallbackResult = fallbackRepository.viewListing(databaseListingId)
                    if (fallbackResult.isSuccess()) {
                        fallbackResult
                    } else {
                        Result.Error(Exception(mapMarketplaceErrorMessage(error), error))
                    }
                }
            )
        }

        return timedResult ?: Result.Error(Exception("Request timed out. Unable to load listing within 2 seconds."))
    }

    override suspend fun toggleSaved(userId: String, itemId: MarketplaceItemId): Result<Boolean> {
        val userSaved = savedItemIdsByUser.getOrPut(userId) { mutableSetOf() }
        val isSaved = if (userSaved.contains(itemId)) {
            userSaved.remove(itemId)
            false
        } else {
            userSaved.add(itemId)
            true
        }
        return Result.Success(isSaved)
    }

    override suspend fun getSavedItemIds(userId: String): Set<MarketplaceItemId> {
        return savedItemIdsByUser[userId]?.toSet() ?: emptySet()
    }

    companion object {
        const val LISTINGS_TABLE = "listings"
        const val REVIEWS_TABLE = "reviews"
        const val TIMEOUT_MILLIS = 2000L
        const val LISTING_ID_PREFIX = "listing:"

        fun normalizeListingId(listingId: String): String {
            return listingId.removePrefix(LISTING_ID_PREFIX)
        }

        private val ERROR_RULES = listOf(
            listOf("could not find the table", "schema cache") to
                "Database table 'listings' not found. Please verify your Supabase schema setup.",
            listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
                "Unable to connect to server. Please check your internet connection.",
            listOf("jwt", "unauthorized", "invalid api key", "no api key") to
                "Unauthorized database request. Please check your Supabase API credentials.",
            listOf("row-level security", "rls") to
                "Database permission denied. Please check your Supabase RLS policies.",
            listOf("invalid input syntax for type uuid", "22p02") to
                "Invalid listing identifier format."
        )

        fun mapMarketplaceErrorMessage(throwable: Throwable): String {
            val message = throwable.message ?: return "An unexpected error occurred while loading the listing."
            val lower = message.lowercase()

            for ((patterns, mappedMessage) in ERROR_RULES) {
                if (patterns.any { lower.contains(it) }) {
                    return mappedMessage
                }
            }

            val firstLine = message.lines().firstOrNull { it.isNotBlank() }?.trim()
                ?: "Failed to load listing details."
            val isTechnicalDump = firstLine.startsWith("url:", ignoreCase = true) ||
                firstLine.startsWith("headers:", ignoreCase = true) ||
                firstLine.startsWith("http method:", ignoreCase = true)

            return if (isTechnicalDump) "Failed to load listing from server." else firstLine
        }
    }

    private fun Throwable.rethrowIfCancellation() {
        if (this is CancellationException) {
            throw this
        }
    }
}
