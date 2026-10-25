package app.stil.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.stil.launcher.core.LauncherSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val Context.launcherDataStore by preferencesDataStore("launcher", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

class SettingsStore(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("settings_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val settings: Flow<LauncherSettings> = store.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { decode(it[key]) }

    private fun decode(value: String?): LauncherSettings = try {
        value?.let { json.decodeFromString<LauncherSettings>(it).normalized() } ?: LauncherSettings()
    } catch (_: SerializationException) {
        LauncherSettings()
    } catch (_: IllegalArgumentException) {
        LauncherSettings()
    }

    suspend fun current(): LauncherSettings = settings.first()

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        store.edit { preferences ->
            preferences[key] = json.encodeToString(transform(decode(preferences[key])).normalized())
        }
    }
}
