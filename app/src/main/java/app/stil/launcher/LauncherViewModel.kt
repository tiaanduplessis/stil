package app.stil.launcher

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.stil.launcher.core.EditError
import app.stil.launcher.core.LaunchPause
import app.stil.launcher.core.LaunchableApp
import app.stil.launcher.core.LauncherRules
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.ProfileKind
import app.stil.launcher.core.SettingsEdit
import app.stil.launcher.data.CatalogState
import app.stil.launcher.data.WallpaperUpdate
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { HOME, APPS, SETTINGS, FONT, WALLPAPER, MANAGE, EDIT, HIDDEN, SETUP, DEFAULT, PAUSE }
data class AppState(val settings: LauncherSettings = LauncherSettings(), val catalog: CatalogState = CatalogState(), val ready: Boolean = false)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val graph = (application as StilApplication).graph
    val state = combine(graph.settings.settings, graph.catalog.state) { settings, catalog -> AppState(settings, catalog, catalog.loaded) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())
    val messages = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    var screen by mutableStateOf(Screen.HOME); private set
    var editId by mutableStateOf<String?>(null); private set
    var pending by mutableStateOf<LaunchableApp?>(null); private set
    var remaining by mutableIntStateOf(3); private set
    var selection by mutableStateOf<List<String>>(emptyList()); private set
    var busy by mutableStateOf(false); private set
    private var setupSeeded = false
    private var pause: LaunchPause? = null
    private var pauseJob: Job? = null
    private var beforePause = Screen.APPS

    fun navigate(target: Screen) {
        pauseJob?.cancel()
        pending = null
        pause = null
        if (target == Screen.SETUP) {
            selection = state.value.settings.pins
            setupSeeded = true
        }
        screen = target
    }

    fun prepareSetup() {
        if (!setupSeeded && state.value.catalog.loaded) {
            val apps = state.value.catalog.apps.filter { it.profile == ProfileKind.PERSONAL && it.available }
            val preferred = listOf("dialer", "messaging", "maps", "camera", "spotify")
            selection = (preferred.mapNotNull { name -> apps.firstOrNull { it.packageName.contains(name) } } + apps)
                .distinctBy { it.id }.take(5).map { it.id }
            setupSeeded = true
        }
    }

    fun toggleSelection(id: String) {
        if (id in selection) selection = selection - id
        else if (selection.size < LauncherSettings.MAX_PINS) selection = selection + id
        else messages.tryEmit(R.string.home_full)
    }

    fun finishSetup() {
        viewModelScope.launch {
            try {
                var success = false
                graph.settings.update { settings ->
                    LauncherRules.finishSetup(settings, selection.mapNotNull { id -> state.value.catalog.apps.find { it.id == id } })
                        .also { success = it.error == null }.settings
                }
                if (success) screen = Screen.DEFAULT else messages.emit(R.string.choose_one_app)
            } catch (_: IOException) { messages.emit(R.string.save_failed) }
        }
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings): Job = viewModelScope.launch {
        try { graph.settings.update(transform) } catch (_: IOException) { messages.emit(R.string.save_failed) }
    }

    fun edit(transform: (LauncherSettings) -> SettingsEdit): Job = viewModelScope.launch {
        try {
            var error: EditError? = null
            graph.settings.update { transform(it).also { result -> error = result.error }.settings }
            error?.let { messages.emit(when (it) {
                EditError.HOME_FULL -> R.string.home_full
                EditError.LAST_HOME_APP -> R.string.keep_one_app
                EditError.EMPTY_NAME -> R.string.enter_name
                EditError.PRIVATE_HOME_APP -> R.string.private_no_pin
                EditError.NO_HOME_APPS -> R.string.choose_one_app
            }) }
        } catch (_: IOException) { messages.emit(R.string.save_failed) }
    }

    fun editApp(app: LaunchableApp) { editId = app.id; navigate(Screen.EDIT) }

    fun open(app: LaunchableApp) {
        if (!app.available) { messages.tryEmit(R.string.app_unavailable); return }
        if (app.id !in state.value.settings.paused) { launch(app); return }
        beforePause = screen
        pending = app
        pause = LaunchPause(SystemClock.elapsedRealtime())
        remaining = 3
        screen = Screen.PAUSE
        pauseJob?.cancel()
        pauseJob = viewModelScope.launch {
            while (remaining > 0) {
                remaining = pause?.secondsRemaining(SystemClock.elapsedRealtime()) ?: 0
                delay(100)
            }
        }
    }

    fun confirmOpen() {
        val app = pending ?: return
        if (pause?.canOpen(SystemClock.elapsedRealtime()) != true) return
        launch(app)
    }

    fun cancelPause() = navigate(beforePause)

    private fun launch(app: LaunchableApp) {
        if (!graph.catalog.launch(app)) messages.tryEmit(R.string.app_unavailable)
        navigate(Screen.HOME)
    }

    fun refresh() {
        graph.catalog.refresh()
        viewModelScope.launch { graph.wallpapers.schedule(graph.settings.current()) }
    }

    fun nextWallpaper() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                messages.emit(when (graph.wallpapers.rotate(force = true)) {
                    WallpaperUpdate.CHANGED -> R.string.wallpaper_updated
                    WallpaperUpdate.WAITING_FOR_WIFI -> R.string.waiting_wifi
                    WallpaperUpdate.RETRY -> R.string.wallpaper_failed
                    WallpaperUpdate.NOT_ALLOWED -> R.string.wallpaper_not_allowed
                    WallpaperUpdate.UNCHANGED -> R.string.wallpaper_unchanged
                })
            } finally { busy = false }
        }
    }
}
