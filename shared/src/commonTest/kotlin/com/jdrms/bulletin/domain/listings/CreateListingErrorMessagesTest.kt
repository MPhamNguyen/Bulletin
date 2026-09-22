package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateListingErrorMessagesTest {

    @Test
    fun validationErrorsRemainActionable() {
        assertEquals(
            "Listing title must be at least 3 characters.",
            CreateListingErrorMessages.toUserMessage(
                IllegalArgumentException("Listing title must be at least 3 characters.")
            )
        )
    }

    @Test
    fun unexpectedErrorsUseGenericMessage() {
        assertEquals(
            CreateListingErrorMessages.GENERIC_FAILURE,
            CreateListingErrorMessages.toUserMessage(RuntimeException("PostgREST returned 42501"))
        )
    }
}
