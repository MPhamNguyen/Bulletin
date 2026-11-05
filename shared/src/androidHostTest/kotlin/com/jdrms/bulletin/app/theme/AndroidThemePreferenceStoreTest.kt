package com.jdrms.bulletin.app.theme

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
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

    @Test
    fun storesCustomColorPerUserAndRestoresLastAppliedColor() = runTest {
        val store = createStore()
        val user1 = UserId("user-1")
        val user2 = UserId("user-2")

        val forest = BulletinThemeColor(Color(0xFF356859).value)
        val custom = BulletinThemeColor(Color(0xFF123ABC).value)
        store.setThemeColor(user1, forest)
        assertEquals(forest, store.themeColorFor(user1))
        assertEquals(forest, store.lastAppliedThemeColor())
        assertEquals(BulletinThemeColor.CLASSIC, store.themeColorFor(user2))

        store.setThemeColor(user2, custom)
        assertEquals(custom, store.themeColorFor(user2))
        assertEquals(forest, store.themeColorFor(user1))
    }

    @Test
    fun storesBaseAndAccentColorsInOneDataStoreTransaction() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val store = AndroidThemePreferenceStore(dataStore)
        val user = UserId("user-1")
        val baseColor = BulletinThemeColor(Color(0xFFF5F5F5).value)
        val accentColor = BulletinThemeColor(Color(0xFF123ABC).value)

        store.setThemeColors(user, baseColor, accentColor)

        assertEquals(1, dataStore.writeCount)
        assertEquals(baseColor, store.baseColorFor(user))
        assertEquals(accentColor, store.themeColorFor(user))
        assertEquals(baseColor, store.lastAppliedBaseColor())
        assertEquals(accentColor, store.lastAppliedThemeColor())
    }

    private fun createStore(): AndroidThemePreferenceStore =
        AndroidThemePreferenceStore(InMemoryPreferencesDataStore())
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow<Preferences>(emptyPreferences())
    var writeCount = 0
        private set

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        writeCount += 1
        val updatedPreferences = transform(preferences.value)
        preferences.value = updatedPreferences
        return updatedPreferences
    }
}
