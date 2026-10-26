package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId

/** Saves a listing for a user. Expected failures carry [SaveListingFailure] in [Result.Error]. */
interface SaveListingPort {
    suspend fun save(userId: String, listingId: HomeItemId): Result<Unit>
}

sealed class SaveListingFailure(cause: Throwable? = null) : Exception(null, cause) {
    class NotSignedIn : SaveListingFailure()
    class ListingUnavailable : SaveListingFailure()
    class Network(cause: Throwable? = null) : SaveListingFailure(cause)
    class Unknown(cause: Throwable? = null) : SaveListingFailure(cause)
}
