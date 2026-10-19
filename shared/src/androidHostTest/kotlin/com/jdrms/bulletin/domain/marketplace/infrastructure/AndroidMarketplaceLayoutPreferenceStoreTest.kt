package com.jdrms.bulletin.domain.marketplace.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidMarketplaceLayoutPreferenceStoreTest {

    @Test
    fun defaultsToOneListingPerRow() = runTest {
        val store = AndroidMarketplaceLayoutPreferenceStore(InMemoryMarketplacePreferencesDataStore())

        assertEquals(MarketplaceLayoutPreference.ONE_PER_ROW, store.get())
    }

    @Test
    fun savedLayoutIsAvailableToARecreatedStore() = runTest {
        val dataStore = InMemoryMarketplacePreferencesDataStore()
        AndroidMarketplaceLayoutPreferenceStore(dataStore).set(MarketplaceLayoutPreference.THREE_PER_ROW)

        val recreatedStore = AndroidMarketplaceLayoutPreferenceStore(dataStore)

        assertEquals(MarketplaceLayoutPreference.THREE_PER_ROW, recreatedStore.get())
    }
}

private class InMemoryMarketplacePreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updatedPreferences = transform(preferences.value)
        preferences.value = updatedPreferences
        return updatedPreferences
    }
}
