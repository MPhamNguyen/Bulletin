package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class SupabaseMessageWriteFailureTest {
    @Test
    fun saleDuringSendOrEditBecomesAnExplicitReadOnlyFailure() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.BadRequest) })
        try {
            val response = client.get("https://example.test")
            val failure = PostgrestRestException("private", null, null, "BM006", response)
            for (isEdit in listOf(false, true)) {
                val translated = assertIs<ConversationReadOnlyException>(failure.toMessageWriteFailure(isEdit))
                assertEquals(ConversationLockReason.LISTING_SOLD, translated.reason)
            }
            val unavailable = PostgrestRestException("private", null, null, "BM001", response)
            assertEquals(
                MessageEditFailure.UNAVAILABLE,
                assertIs<MessageEditException>(unavailable.toMessageWriteFailure(isEdit = true)).failure
            )
            assertSame(unavailable, unavailable.toMessageWriteFailure(isEdit = false))
            val unknown = PostgrestRestException("private", null, null, "23503", response)
            assertSame(unknown, unknown.toMessageWriteFailure(isEdit = true))
        } finally {
            client.close()
        }
    }
}
