package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage

/** Resolves the seller outside Home. Expected failures carry [ContactSellerFailure] in [Result.Error]. */
interface ContactSellerPort {
    suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit>
}

sealed class ContactSellerFailure(cause: Throwable? = null) : Exception(null, cause) {
    class NotSignedIn : ContactSellerFailure()
    class ListingUnavailable : ContactSellerFailure()
    class OwnListing : ContactSellerFailure()
    class Network(cause: Throwable? = null) : ContactSellerFailure(cause)
    class Unknown(cause: Throwable? = null) : ContactSellerFailure(cause)
}
