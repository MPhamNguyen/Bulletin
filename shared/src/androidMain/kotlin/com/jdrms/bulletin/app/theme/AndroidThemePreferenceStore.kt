package com.jdrms.bulletin.app.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
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
) : ThemePreferenceStores {

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

    override suspend fun lastAppliedThemeColor(): BulletinThemeColor {
        val prefs = dataStore.data
            .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
            .first()
        return themeColorFromStoredValue(prefs[LAST_APPLIED_THEME_COLOR_KEY]) ?: BulletinThemeColor.CLASSIC
    }

    override suspend fun setLastAppliedThemeColor(themeColor: BulletinThemeColor) {
        dataStore.edit { prefs -> prefs[LAST_APPLIED_THEME_COLOR_KEY] = themeColor.value.toString(16) }
    }

    override suspend fun themeColorFor(userId: UserId): BulletinThemeColor {
        val prefs = dataStore.data
            .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
            .first()
        return themeColorFromStoredValue(prefs[themeColorKeyFor(userId)]) ?: BulletinThemeColor.CLASSIC
    }

    override suspend fun setThemeColor(userId: UserId, themeColor: BulletinThemeColor) {
        dataStore.edit { prefs ->
            val storedColor = themeColor.value.toString(16)
            prefs[themeColorKeyFor(userId)] = storedColor
            prefs[LAST_APPLIED_THEME_COLOR_KEY] = storedColor
        }
    }

    override suspend fun lastAppliedBaseColor(): BulletinThemeColor {
        val prefs = dataStore.data
            .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
            .first()
        return themeColorFromStoredValue(prefs[LAST_APPLIED_BASE_COLOR_KEY]) ?: BulletinThemeColor.BASE
    }

    override suspend fun setLastAppliedBaseColor(baseColor: BulletinThemeColor) {
        dataStore.edit { prefs -> prefs[LAST_APPLIED_BASE_COLOR_KEY] = baseColor.value.toString(16) }
    }

    override suspend fun baseColorFor(userId: UserId): BulletinThemeColor {
        val prefs = dataStore.data
            .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
            .first()
        return themeColorFromStoredValue(prefs[baseColorKeyFor(userId)]) ?: BulletinThemeColor.BASE
    }

    override suspend fun setBaseColor(userId: UserId, baseColor: BulletinThemeColor) {
        dataStore.edit { prefs ->
            val storedColor = baseColor.value.toString(16)
            prefs[baseColorKeyFor(userId)] = storedColor
            prefs[LAST_APPLIED_BASE_COLOR_KEY] = storedColor
        }
    }

    override suspend fun setThemeColors(
        userId: UserId,
        baseColor: BulletinThemeColor,
        themeColor: BulletinThemeColor
    ) {
        dataStore.edit { prefs ->
            val storedBaseColor = baseColor.value.toString(16)
            val storedThemeColor = themeColor.value.toString(16)
            prefs[baseColorKeyFor(userId)] = storedBaseColor
            prefs[LAST_APPLIED_BASE_COLOR_KEY] = storedBaseColor
            prefs[themeColorKeyFor(userId)] = storedThemeColor
            prefs[LAST_APPLIED_THEME_COLOR_KEY] = storedThemeColor
        }
    }

    override suspend fun setLastAppliedThemeColors(baseColor: BulletinThemeColor, themeColor: BulletinThemeColor) {
        dataStore.edit { prefs ->
            prefs[LAST_APPLIED_BASE_COLOR_KEY] = baseColor.value.toString(16)
            prefs[LAST_APPLIED_THEME_COLOR_KEY] = themeColor.value.toString(16)
        }
    }

    private fun preferenceFromStoredValue(value: String?): ThemePreference {
        return ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.SYSTEM
    }

    private fun themeColorFromStoredValue(value: String?): BulletinThemeColor? =
        value?.toULongOrNull(16)?.let(::BulletinThemeColor)

    private fun keyFor(userId: UserId): Preferences.Key<String> = stringPreferencesKey("$KEY_PREFIX${userId.value}")
    private fun themeColorKeyFor(userId: UserId): Preferences.Key<String> =
        stringPreferencesKey("$THEME_COLOR_KEY_PREFIX${userId.value}")
    private fun baseColorKeyFor(userId: UserId): Preferences.Key<String> =
        stringPreferencesKey("$BASE_COLOR_KEY_PREFIX${userId.value}")

    private companion object {
        const val KEY_PREFIX = "theme_"
        const val THEME_COLOR_KEY_PREFIX = "theme_color_"
        const val BASE_COLOR_KEY_PREFIX = "theme_base_color_"
        val LAST_APPLIED_KEY = stringPreferencesKey("last_applied_theme")
        val LAST_APPLIED_THEME_COLOR_KEY = stringPreferencesKey("last_applied_theme_color")
        val LAST_APPLIED_BASE_COLOR_KEY = stringPreferencesKey("last_applied_theme_base_color")
    }
}
