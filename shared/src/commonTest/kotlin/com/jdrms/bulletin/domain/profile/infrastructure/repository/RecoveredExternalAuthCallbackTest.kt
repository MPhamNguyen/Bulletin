package com.jdrms.bulletin.domain.profile.infrastructure.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecoveredExternalAuthCallbackTest {
    @Test
    fun consumesOnlyOnceAndClearsExplicitly() {
        val recovered = RecoveredExternalAuthCallback { 100L }
        val callback = ExternalAuthCallback.Code("code")
        recovered.store(callback, 0L)
        assertTrue(recovered.isPending)
        assertEquals(callback, recovered.consume())
        assertFalse(recovered.isPending)
        assertNull(recovered.consume())
        recovered.store(callback, 0L)
        recovered.clear()
        assertNull(recovered.consume())
    }

    @Test
    fun expiresAtOriginalRequestDeadlineEvenWhenCallbackArrivedLate() {
        var now = ExternalAuthCallbackReceiver.LIFETIME_MS
        val recovered = RecoveredExternalAuthCallback { now }
        recovered.store(ExternalAuthCallback.Code("code"), 0L)
        assertTrue(recovered.isPending)
        now++
        assertFalse(recovered.isPending)
        assertNull(recovered.consume())
        now = 0L
        assertFalse(recovered.isPending)
    }
}
