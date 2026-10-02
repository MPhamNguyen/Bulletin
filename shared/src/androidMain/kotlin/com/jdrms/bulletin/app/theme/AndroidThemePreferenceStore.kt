package com.jdrms.bulletin.app.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bulletin_theme_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class AndroidThemePreferenceStore(
    private val dataStore: DataStore<Preferences>
) : ThemePreferenceStore {

    constructor(context: Context) : this(context.themeDataStore)

    override suspend fun lastAppliedPreference(): ThemePreference {
        val prefs = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        return preferenceFromStoredValue(prefs[LAST_APPLIED_KEY])
    }

    override suspend fun setLastAppliedPreference(preference: ThemePreference) {
        dataStore.edit { prefs ->
            prefs[LAST_APPLIED_KEY] = preference.name
        }
    }

    override suspend fun preferenceFor(userId: UserId): ThemePreference {
        val prefs = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        return preferenceFromStoredValue(prefs[keyFor(userId)])
    }

    override suspend fun setPreference(userId: UserId, preference: ThemePreference) {
        dataStore.edit { prefs ->
            prefs[keyFor(userId)] = preference.name
            prefs[LAST_APPLIED_KEY] = preference.name
        }
    }

    private fun preferenceFromStoredValue(value: String?): ThemePreference {
        return ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.SYSTEM
    }

    private fun keyFor(userId: UserId): Preferences.Key<String> = stringPreferencesKey("$KEY_PREFIX${userId.value}")

    private companion object {
        const val KEY_PREFIX = "theme_"
        val LAST_APPLIED_KEY = stringPreferencesKey("last_applied_theme")
    }
}
