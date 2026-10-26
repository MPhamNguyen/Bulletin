package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

internal enum class HomeAdapterFailure { NOT_SIGNED_IN, UNAVAILABLE, OWN_LISTING, NETWORK, UNKNOWN }

/** Translate legacy context failures here; never return the raw exception or its potentially private cause. */
internal fun classifyHomeAdapterFailure(exception: Throwable): HomeAdapterFailure {
    val causes = generateSequence(exception) { it.cause }.take(16).toList()
    causes.filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }
    return causes.firstNotNullOfOrNull(::knownHomeAdapterFailure) ?: HomeAdapterFailure.UNKNOWN
}

private fun knownHomeAdapterFailure(exception: Throwable): HomeAdapterFailure? = when (exception) {
    is MessagingAuthenticationRequiredException -> HomeAdapterFailure.NOT_SIGNED_IN
    is ConversationAccessException, is NoSuchElementException -> HomeAdapterFailure.UNAVAILABLE
    is IOException, is HttpRequestTimeoutException -> HomeAdapterFailure.NETWORK
    is PostgrestRestException -> when (exception.code) {
        "23503", "PGRST116" -> HomeAdapterFailure.UNAVAILABLE
        else -> httpFailure(exception.statusCode)
    }
    is RestException -> httpFailure(exception.statusCode)
    else -> legacyHomeAdapterFailure(exception.message)
}

private fun httpFailure(status: Int): HomeAdapterFailure? = when (status) {
    401 -> HomeAdapterFailure.NOT_SIGNED_IN
    404 -> HomeAdapterFailure.UNAVAILABLE
    408, 429, in 500..599 -> HomeAdapterFailure.NETWORK
    else -> null
}

// These exact messages are emitted by the existing public use cases and adapters without typed failures.
private fun legacyHomeAdapterFailure(message: String?): HomeAdapterFailure? = when (message) {
    "You cannot message yourself." -> HomeAdapterFailure.OWN_LISTING
    "This listing is unavailable.", "Listing not found." -> HomeAdapterFailure.UNAVAILABLE
    "Bookmark request timed out.", "Request timed out. Unable to load listing within 2 seconds.",
    "Unable to connect to server. Please check your internet connection." -> HomeAdapterFailure.NETWORK
    else -> null
}
