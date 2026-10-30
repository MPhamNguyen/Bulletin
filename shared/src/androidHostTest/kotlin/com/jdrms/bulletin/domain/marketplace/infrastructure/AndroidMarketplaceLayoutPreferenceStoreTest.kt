package com.jdrms.bulletin.domain.marketplace.infrastructure

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidMarketplaceLayoutPreferenceStoreTest {

    @Test
    fun defaultsToTwoListingsPerRow() = runTest {
        val store = AndroidMarketplaceLayoutPreferenceStore(InMemoryPreferencesDataStore())

        assertEquals(MarketplaceLayoutPreference.TWO_PER_ROW, store.get())
    }

    @Test
    fun savedLayoutIsAvailableToARecreatedStore() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        AndroidMarketplaceLayoutPreferenceStore(dataStore).set(MarketplaceLayoutPreference.ONE_PER_ROW)

        val recreatedStore = AndroidMarketplaceLayoutPreferenceStore(dataStore)

        assertEquals(MarketplaceLayoutPreference.ONE_PER_ROW, recreatedStore.get())
    }

    @Test
    fun removedThreeColumnPreferenceMigratesToTwoColumns() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("listings_per_row")] = "THREE_PER_ROW"
        }

        val store = AndroidMarketplaceLayoutPreferenceStore(dataStore)

        assertEquals(MarketplaceLayoutPreference.TWO_PER_ROW, store.get())
    }
}
