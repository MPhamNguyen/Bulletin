package com.jdrms.bulletin.app.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferencesTest {
    @Test
    fun disablingPersistsPreferenceAndClearsExistingAlerts() {
        var saved = true
        var clears = 0
        val preferences = NotificationPreferences(persist = { saved = it }, clearNotifications = { clears++ })
        assertTrue(preferences.enabled.value)

        preferences.setEnabled(false)

        assertFalse(saved)
        assertFalse(preferences.enabled.value)
        assertEquals(1, clears)
        val restored = NotificationPreferences(initiallyEnabled = saved)
        assertFalse(restored.enabled.value)

        preferences.setEnabled(true)
        assertTrue(saved)
        assertTrue(preferences.enabled.value)
        assertEquals(1, clears)
    }
}
