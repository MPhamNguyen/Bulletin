package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationException
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy

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
    private val listingsRepository: ListingsRepository
) {
    suspend fun updateListing(listing: Listing): Result<Listing> {
        return listingsRepository.updateListing(listing)
    }

    suspend fun deleteListing(id: ListingId): Result<Unit> {
        return listingsRepository.deleteListing(id)
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
