package com.jdrms.bulletin.domain.home.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SwipeDecisionTest {
    @Test
    fun equalityIncludesListingAndVerdict() {
        val decision = SwipeDecision(HomeItemId("listing"), SwipeVerdict.SAVE)

        assertEquals(decision, SwipeDecision(HomeItemId("listing"), SwipeVerdict.SAVE))
        assertEquals(decision.hashCode(), SwipeDecision(HomeItemId("listing"), SwipeVerdict.SAVE).hashCode())
        assertNotEquals(decision, SwipeDecision(HomeItemId("other"), SwipeVerdict.SAVE))
        assertNotEquals(decision, SwipeDecision(HomeItemId("listing"), SwipeVerdict.PASS))
    }

    @Test
    fun supportsSaveAndPass() {
        assertEquals(setOf(SwipeVerdict.SAVE, SwipeVerdict.PASS), SwipeVerdict.entries.toSet())
        SwipeVerdict.entries.forEach { verdict ->
            assertEquals(verdict, SwipeDecision(HomeItemId("listing"), verdict).verdict)
        }
    }
}
