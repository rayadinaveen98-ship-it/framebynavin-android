package com.framebynavin.app.personal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Separate from Creator and personal reminder records. Safe default: OFF. */
private val Context.personalFollowUpsData by preferencesDataStore(name = "backlot_personal_followups_v1")

internal data class PersonalFollowUpSettings(val enabled: Boolean = false, val delayMinutes: Int = 60) {
    fun checked(): PersonalFollowUpSettings {
        require(delayMinutes in setOf(30, 60, 120)) { "Choose a supported follow-up delay" }
        return this
    }
}

internal class PersonalFollowUpSettingsStore(context: Context) {
    private val data = context.applicationContext.personalFollowUpsData
    private val enabledKey = booleanPreferencesKey("followups_enabled_v1")
    private val delayKey = intPreferencesKey("followups_delay_minutes_v1")
    val settings: Flow<PersonalFollowUpSettings> = data.data.map {
        PersonalFollowUpSettings(
            enabled = it[enabledKey] ?: false,
            delayMinutes = (it[delayKey] ?: 60).takeIf { d -> d in setOf(30, 60, 120) } ?: 60,
        )
    }
    suspend fun current() = settings.first()
    suspend fun setEnabled(value: Boolean) { data.edit { it[enabledKey] = value } }
    suspend fun setDelay(minutes: Int) {
        require(minutes in setOf(30, 60, 120))
        data.edit { it[delayKey] = minutes }
    }
}
