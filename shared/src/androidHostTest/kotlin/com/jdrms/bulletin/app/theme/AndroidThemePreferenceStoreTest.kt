package com.jdrms.bulletin.app.theme

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidThemePreferenceStoreTest {

    @Test
    fun defaultsToSystemWhenNoPreferenceSaved() = runTest {
        val tempFile = File.createTempFile("test_theme_prefs", ".preferences_pb").apply { deleteOnExit() }
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        val store = AndroidThemePreferenceStore(dataStore)

        assertEquals(ThemePreference.SYSTEM, store.lastAppliedPreference())
        assertEquals(ThemePreference.SYSTEM, store.preferenceFor(UserId("user-1")))
    }

    @Test
    fun setsAndRetrievesLastAppliedPreference() = runTest {
        val tempFile = File.createTempFile("test_theme_prefs", ".preferences_pb").apply { deleteOnExit() }
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        val store = AndroidThemePreferenceStore(dataStore)

        store.setLastAppliedPreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, store.lastAppliedPreference())

        store.setLastAppliedPreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, store.lastAppliedPreference())
    }

    @Test
    fun setsAndRetrievesPerUserPreferenceAndUpdatesLastApplied() = runTest {
        val tempFile = File.createTempFile("test_theme_prefs", ".preferences_pb").apply { deleteOnExit() }
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        val store = AndroidThemePreferenceStore(dataStore)

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
}
