package app.stil.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.stil.launcher.core.LaunchHistory
import app.stil.launcher.core.LaunchableApp
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class LaunchStatsStore(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("launch_stats_v1")
    private val json = Json { ignoreUnknownKeys = true }
    val history = store.data.map { decode(it[key]).retained(LocalDate.now()) }

    private fun decode(value: String?): LaunchHistory = try {
        value?.let { json.decodeFromString<LaunchHistory>(it) } ?: LaunchHistory()
    } catch (_: SerializationException) {
        LaunchHistory()
    } catch (_: IllegalArgumentException) {
        LaunchHistory()
    }

    suspend fun record(app: LaunchableApp, today: LocalDate = LocalDate.now()) {
        store.edit { preferences ->
            preferences[key] = json.encodeToString(decode(preferences[key]).record(app, today))
        }
    }
}
