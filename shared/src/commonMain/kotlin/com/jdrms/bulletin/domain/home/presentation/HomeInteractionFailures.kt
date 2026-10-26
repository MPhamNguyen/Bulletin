package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SendQuickMessageFailure
import com.jdrms.bulletin.domain.home.domain.model.SellerMessageFailure
import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught") // Adapter/port exceptions become typed presentation failures.
internal suspend fun <T> interactionAttempt(block: suspend () -> Result<T>): Result<T> = try {
    block().also { result ->
        if (result is Result.Error) {
            generateSequence(result.exception) { it.cause }.take(16)
                .filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }
        }
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (exception: Exception) {
    Result.Error(exception)
}

internal fun messageError(failure: Throwable): HomeInteractionError = when (failure) {
    is SendQuickMessageFailure.Validation -> validationError(failure.reason)
    is SendQuickMessageFailure.NotSignedIn -> HomeInteractionError.NOT_SIGNED_IN
    is SendQuickMessageFailure.ListingUnavailable -> HomeInteractionError.LISTING_UNAVAILABLE
    is SendQuickMessageFailure.OwnListing -> HomeInteractionError.OWN_LISTING
    is SendQuickMessageFailure.Network -> HomeInteractionError.NETWORK
    else -> HomeInteractionError.UNKNOWN
}

internal fun validationError(failure: Throwable): HomeInteractionError = when (failure) {
    is SellerMessageFailure.Blank -> HomeInteractionError.BLANK_MESSAGE
    is SellerMessageFailure.TooLong -> HomeInteractionError.MESSAGE_TOO_LONG
    else -> HomeInteractionError.UNKNOWN
}
