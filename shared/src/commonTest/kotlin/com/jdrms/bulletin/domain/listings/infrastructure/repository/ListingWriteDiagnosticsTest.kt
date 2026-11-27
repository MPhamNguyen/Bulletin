package com.jdrms.bulletin.domain.listings.infrastructure.repository

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListingWriteDiagnosticsTest {
    @Test
    fun apiFailuresExposeStageStatusAndCodeWithoutHeadersOrContent() = runTest {
        val error = apiFailure("PGRST204")
        assertEquals(
            "listing_write_failed stage=edit_rpc status=400 code=PGRST204 category=http",
            listingWriteFailureDiagnostic(ListingWriteStage.EDIT_RPC, error)
        )
    }

    @Test
    fun arbitraryBackendCodesCannotLeakIntoLogs() = runTest {
        val error = apiFailure("private-token-or-content")
        assertEquals(
            "listing_write_failed stage=routine_update status=400 code=unavailable category=http",
            listingWriteFailureDiagnostic(ListingWriteStage.ROUTINE_UPDATE, error)
        )
    }

    @Test
    fun clientFailuresDoNotPrintTheirMessages() {
        assertEquals(
            "listing_write_failed stage=read_after_write status=unavailable code=unavailable category=serialization",
            listingWriteFailureDiagnostic(
                ListingWriteStage.READ_AFTER_WRITE,
                SerializationException("private listing content")
            )
        )
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingAGenericSaveError() {
        assertFailsWith<CancellationException> {
            reportListingWriteFailure(ListingWriteStage.READ_BEFORE_WRITE, CancellationException())
        }
    }

    private suspend fun apiFailure(code: String): PostgrestRestException {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.BadRequest) })
        return try {
            val response = client.get("https://example.test/private-path?private=value") {
                header("Authorization", "Bearer private-token")
            }
            PostgrestRestException("private content", "private hint", "private details", code, response)
        } finally {
            client.close()
        }
    }
}
