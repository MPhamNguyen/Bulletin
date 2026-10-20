package com.jdrms.bulletin.domain.marketplace.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.marketplaceLayoutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bulletin_marketplace_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class AndroidMarketplaceLayoutPreferenceStore(
    private val dataStore: DataStore<Preferences>
) : MarketplaceLayoutPreferenceStore {

    constructor(context: Context) : this(context.marketplaceLayoutDataStore)

    override suspend fun get(): MarketplaceLayoutPreference {
        val preferences = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        val storedValue = preferences[LAYOUT_KEY]
        if (storedValue == LEGACY_THREE_PER_ROW) return MarketplaceLayoutPreference.TWO_PER_ROW
        return MarketplaceLayoutPreference.entries.firstOrNull { it.name == storedValue }
            ?: MarketplaceLayoutPreference.TWO_PER_ROW
    }

    override suspend fun set(preference: MarketplaceLayoutPreference) {
        dataStore.edit { preferences -> preferences[LAYOUT_KEY] = preference.name }
    }

    private companion object {
        const val LEGACY_THREE_PER_ROW = "THREE_PER_ROW"
        val LAYOUT_KEY = stringPreferencesKey("listings_per_row")
    }
}
