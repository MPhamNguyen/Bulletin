package com.jdrms.bulletin.domain.listings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class CreateListingStepDirectionTest {
    @Test
    fun movingToALaterStepSlidesForward() {
        assertEquals(1, stepDirection(CreateListingStep.PHOTOS, CreateListingStep.DETAILS))
        assertEquals(1, stepDirection(CreateListingStep.PHOTOS, CreateListingStep.PRICE))
    }

    @Test
    fun movingToAnEarlierStepSlidesBackward() {
        assertEquals(-1, stepDirection(CreateListingStep.PRICE, CreateListingStep.DETAILS))
        assertEquals(-1, stepDirection(CreateListingStep.PRICE, CreateListingStep.PHOTOS))
    }
}
