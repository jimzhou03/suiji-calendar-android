package com.jimzhou03.suijicalendar.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val remindersKey = booleanPreferencesKey("reminders_master_enabled")

    val remindersEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[remindersKey] ?: true }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[remindersKey] = enabled }
    }
}
