package dev.slate.ai.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slate_prefs")

@Singleton
class SlatePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val OFFLINE_ONLY = booleanPreferencesKey("offline_only")
        val CHAT_HISTORY_ENABLED = booleanPreferencesKey("chat_history_enabled")
        val LAST_LOADED_MODEL_ID = stringPreferencesKey("last_loaded_model_id")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true // Dark mode first
    }

    val isOfflineOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_ONLY] ?: false
    }

    val isChatHistoryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CHAT_HISTORY_ENABLED] ?: true
    }

    val lastLoadedModelId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_LOADED_MODEL_ID]
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setOfflineOnly(offline: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_ONLY] = offline }
    }

    suspend fun setChatHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CHAT_HISTORY_ENABLED] = enabled }
    }

    suspend fun setLastLoadedModelId(modelId: String?) {
        context.dataStore.edit {
            if (modelId != null) {
                it[Keys.LAST_LOADED_MODEL_ID] = modelId
            } else {
                it.remove(Keys.LAST_LOADED_MODEL_ID)
            }
        }
    }
}
