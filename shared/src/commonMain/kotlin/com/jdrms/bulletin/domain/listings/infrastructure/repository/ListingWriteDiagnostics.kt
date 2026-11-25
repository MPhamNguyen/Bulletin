package com.jdrms.bulletin.domain.listings.infrastructure.repository

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal enum class ListingWriteStage {
    READ_BEFORE_WRITE,
    EDIT_RPC,
    ROUTINE_UPDATE,
    READ_AFTER_WRITE
}

internal fun reportListingWriteFailure(stage: ListingWriteStage, error: Throwable) {
    if (error is CancellationException) throw error
    println(listingWriteFailureDiagnostic(stage, error))
}

/** SDK exception messages include request headers: never print the exception or its message. */
internal fun listingWriteFailureDiagnostic(stage: ListingWriteStage, error: Throwable): String {
    val status = (error as? RestException)?.statusCode?.toString() ?: "unavailable"
    val code = (error as? PostgrestRestException)?.code
        ?.takeIf { it.matches(Regex("[A-Z0-9]{5}|PGRST[0-9]{3}")) } ?: "unavailable"
    val category = when (error) {
        is RestException -> "http"
        is SerializationException -> "serialization"
        else -> "client"
    }
    return "listing_write_failed stage=${stage.name.lowercase()} status=$status code=$code category=$category"
}
