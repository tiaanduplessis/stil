package app.stil.launcher

import android.app.Application
import app.stil.launcher.data.AppCatalog
import app.stil.launcher.data.SettingsStore
import app.stil.launcher.data.WallpaperRepository
import app.stil.launcher.data.launcherDataStore
import app.stil.launcher.core.ProfileKind
import app.stil.launcher.core.LauncherRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

class StilApplication : Application() {
    val graph by lazy { AppGraph(this) }
    override fun onCreate() {
        super.onCreate()
        graph.start()
    }
}

class AppGraph(application: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(application.launcherDataStore)
    val catalog = AppCatalog(application, scope)
    val wallpapers = WallpaperRepository(application, settings)

    fun start() {
        scope.launch {
            catalog.state.collectLatest { catalog ->
                if (catalog.complete) try {
                    settings.update { current -> LauncherRules.reconcilePins(current, catalog.apps,
                        catalog.profiles.filter { it.quiet && it.kind == ProfileKind.WORK }.map { it.serial }.toSet()) }
                } catch (_: IOException) {
                    android.util.Log.w("Stil", "Could not reconcile home apps")
                }
            }
        }
        scope.launch {
            settings.settings.map { it.wallpaper to it.wifiOnly }.distinctUntilChanged().collectLatest {
                wallpapers.schedule(settings.current())
            }
        }
    }
}
