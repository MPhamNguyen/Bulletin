package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import com.jdrms.bulletin.domain.home.domain.model.SellerMessageFailure

class SendQuickMessage(private val contactSeller: ContactSellerPort) {
    suspend operator fun invoke(userId: String?, listingId: HomeItemId, text: String): Result<Unit> {
        val message = when (val validation = SellerMessage.create(text)) {
            is Result.Success -> validation.data
            is Result.Error -> return Result.Error(
                SendQuickMessageFailure.Validation(validation.exception as SellerMessageFailure)
            )
        }
        if (userId.isNullOrBlank()) return Result.Error(SendQuickMessageFailure.NotSignedIn())
        return when (val result = contactSeller.send(userId, listingId, message)) {
            is Result.Success -> result
            is Result.Error -> Result.Error(mapFailure(result.exception))
        }
    }

    private fun mapFailure(failure: Throwable): SendQuickMessageFailure = when (failure) {
        is ContactSellerFailure.NotSignedIn -> SendQuickMessageFailure.NotSignedIn()
        is ContactSellerFailure.ListingUnavailable -> SendQuickMessageFailure.ListingUnavailable()
        is ContactSellerFailure.OwnListing -> SendQuickMessageFailure.OwnListing()
        is ContactSellerFailure.Network -> SendQuickMessageFailure.Network(failure)
        else -> SendQuickMessageFailure.Unknown(failure)
    }
}

sealed class SendQuickMessageFailure(cause: Throwable? = null) : Exception(null, cause) {
    class Validation(val reason: SellerMessageFailure) : SendQuickMessageFailure(reason)
    class NotSignedIn : SendQuickMessageFailure()
    class ListingUnavailable : SendQuickMessageFailure()
    class OwnListing : SendQuickMessageFailure()
    class Network(cause: Throwable) : SendQuickMessageFailure(cause)
    class Unknown(cause: Throwable) : SendQuickMessageFailure(cause)
}
