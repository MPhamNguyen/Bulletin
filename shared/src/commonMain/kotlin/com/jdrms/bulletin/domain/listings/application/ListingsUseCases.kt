package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationException
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import kotlinx.coroutines.CancellationException

data class ListingSeller(
    val id: SellerId,
    val name: String
)

class AuthenticationRequiredException(cause: Throwable? = null) : Exception(
    "Please sign in again before creating a listing.",
    cause
)

class ListingSellerLookupException(cause: Throwable) : Exception(
    "Unable to load the current seller.",
    cause
)

interface CurrentListingSellerProvider {
    suspend fun getCurrentSeller(): Result<ListingSeller>
}

object CreateListingErrorMessages {
    const val GENERIC_FAILURE = "An Error has Occured, Please Try Again Later"
    const val AUTHENTICATION_REQUIRED = "Please sign in again before creating a listing."

    fun toUserMessage(error: Throwable): String {
        return when {
            error is AuthenticationRequiredException -> AUTHENTICATION_REQUIRED
            error is ListingValidationException -> error.message.orEmpty()
            else -> GENERIC_FAILURE
        }
    }
}

class CreateListing(
    private val listingsRepository: ListingsRepository,
    private val policy: ListingValidationPolicy = ListingValidationPolicy()
) {
    suspend operator fun invoke(listing: Listing): Result<Listing> {
        val validation = policy.validateListing(listing)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return listingsRepository.createListing(listing)
    }
}

class ManageListing(
    private val listingsRepository: ListingsRepository,
    private val policy: ListingValidationPolicy = ListingValidationPolicy()
) {
    suspend fun updateListing(listing: Listing, editorSellerId: SellerId? = null): Result<Listing> {
        var canonicalListing = listing
        if (editorSellerId != null) {
            val persistedListing = listingsRepository.getListing(listing.id)
                ?: return Result.Error(NoSuchElementException("Listing not found with ID: ${listing.id.value}"))
            val ownershipValidation = policy.validateOwnership(persistedListing, editorSellerId)
            if (ownershipValidation.isError()) {
                return Result.Error((ownershipValidation as Result.Error).exception)
            }
            // Identity and server-owned fields always come from storage, never from the request payload.
            canonicalListing = persistedListing.copy(
                title = listing.title,
                description = listing.description,
                price = listing.price,
                category = listing.category,
                condition = listing.condition
            )
        }
        val listingValidation = policy.validateListing(canonicalListing)
        if (listingValidation.isError()) {
            return Result.Error((listingValidation as Result.Error).exception)
        }
        return listingsRepository.updateListing(canonicalListing)
    }
}

class DeleteListing(
    private val listingsRepository: ListingsRepository,
    private val policy: ListingValidationPolicy = ListingValidationPolicy()
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(id: ListingId, sellerId: SellerId): Result<Unit> {
        val listing = try {
            listingsRepository.getListing(id)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(exception)
        }
            ?: return Result.Error(NoSuchElementException("Listing not found with ID: ${id.value}"))
        val ownershipValidation = policy.validateOwnership(listing, sellerId, action = "delete")
        if (ownershipValidation.isError()) {
            return Result.Error((ownershipValidation as Result.Error).exception)
        }
        return listingsRepository.deleteListing(id, sellerId)
    }
}

class GetSellerListings(
    private val listingsRepository: ListingsRepository
) {
    suspend operator fun invoke(sellerId: SellerId): List<Listing> {
        return listingsRepository.getSellerListings(sellerId)
    }

    suspend fun getAll(): List<Listing> {
        return listingsRepository.getAllListings()
    }
}
