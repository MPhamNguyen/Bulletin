package com.jdrms.bulletin.app.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidThemePreferenceStoreTest {

    @Test
    fun defaultsToSystemWhenNoPreferenceSaved() = runTest {
        val store = createStore()

        assertEquals(ThemePreference.SYSTEM, store.lastAppliedPreference())
        assertEquals(ThemePreference.SYSTEM, store.preferenceFor(UserId("user-1")))
    }

    @Test
    fun setsAndRetrievesLastAppliedPreference() = runTest {
        val store = createStore()

        store.setLastAppliedPreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, store.lastAppliedPreference())

        store.setLastAppliedPreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, store.lastAppliedPreference())
    }

    @Test
    fun setsAndRetrievesPerUserPreferenceAndUpdatesLastApplied() = runTest {
        val store = createStore()

        val user1 = UserId("user-1")
        val user2 = UserId("user-2")

        store.setPreference(user1, ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, store.preferenceFor(user1))
        assertEquals(ThemePreference.DARK, store.lastAppliedPreference())
        assertEquals(ThemePreference.SYSTEM, store.preferenceFor(user2))

        store.setPreference(user2, ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, store.preferenceFor(user2))
        assertEquals(ThemePreference.DARK, store.preferenceFor(user1))
        assertEquals(ThemePreference.LIGHT, store.lastAppliedPreference())
    }

    private fun createStore(): AndroidThemePreferenceStore =
        AndroidThemePreferenceStore(InMemoryPreferencesDataStore())
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updatedPreferences = transform(preferences.value)
        preferences.value = updatedPreferences
        return updatedPreferences
    }
}
