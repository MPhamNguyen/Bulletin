package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationException
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateListingErrorMessagesTest {

    @Test
    fun validationErrorsRemainActionable() {
        assertEquals(
            "Listing title must be at least 3 characters.",
            CreateListingErrorMessages.toUserMessage(
                ListingValidationException.TitleTooShort()
            )
        )
    }

    @Test
    fun unrelatedValidationWordingDoesNotBecomeUserFacingListingError() {
        assertEquals(
            CreateListingErrorMessages.GENERIC_FAILURE,
            CreateListingErrorMessages.toUserMessage(IllegalArgumentException("Listing title wording changed"))
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
