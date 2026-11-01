package com.jdrms.bulletin.domain.marketplace.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.reportedListingVisibilityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bulletin_reported_listing_visibility",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class AndroidReportedListingVisibilityPreferenceStore(
    private val dataStore: DataStore<Preferences>
) : ReportedListingVisibilityPreferenceStore {
    constructor(context: Context) : this(context.reportedListingVisibilityDataStore)

    override suspend fun shouldHideReportedListings(reporterId: MarketplaceReporterId): Boolean {
        val preferences = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        return preferences[keyFor(reporterId)] ?: true
    }

    override suspend fun setHideReportedListings(reporterId: MarketplaceReporterId, shouldHide: Boolean) {
        dataStore.edit { preferences ->
            preferences[keyFor(reporterId)] = shouldHide
        }
    }

    private fun keyFor(reporterId: MarketplaceReporterId) =
        booleanPreferencesKey("hide_reported_listings_${reporterId.value}")
}
