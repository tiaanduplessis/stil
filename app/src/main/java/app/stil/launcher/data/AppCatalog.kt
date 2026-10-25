package app.stil.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import app.stil.launcher.core.LaunchableApp
import app.stil.launcher.core.ProfileKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AppProfile(val serial: Long, val kind: ProfileKind, val quiet: Boolean)
data class CatalogState(val apps: List<LaunchableApp> = emptyList(), val profiles: List<AppProfile> = emptyList(), val loaded: Boolean = false, val complete: Boolean = false)

class AppCatalog(private val context: Context, private val scope: CoroutineScope) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)
    private val mutable = MutableStateFlow(CatalogState())
    val state = mutable.asStateFlow()
    private val refreshLock = Mutex()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcher.registerCallback(callback, Handler(Looper.getMainLooper()))
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            if (Build.VERSION.SDK_INT >= 35) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
        }
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refresh()
        }, filter, ContextCompat.RECEIVER_EXPORTED)
        refresh()
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            refreshLock.withLock {
                val profiles = mutableListOf<AppProfile>()
                val apps = mutableListOf<LaunchableApp>()
                var complete = true
                val handles = runCatching { launcher.profiles }.getOrElse { complete = false; listOf(Process.myUserHandle()) }
                handles.forEach { user ->
                    val serial = users.getSerialNumberForUser(user)
                    if (serial < 0) return@forEach
                    val userType = if (Build.VERSION.SDK_INT >= 35) runCatching { launcher.getLauncherUserInfo(user)?.userType }.getOrNull() else null
                    if (Build.VERSION.SDK_INT >= 35 && user != Process.myUserHandle() && userType == null) { complete = false; return@forEach }
                    val kind = when {
                        userType == "android.os.usertype.profile.PRIVATE" -> ProfileKind.PRIVATE
                        user == Process.myUserHandle() -> ProfileKind.PERSONAL
                        else -> ProfileKind.WORK
                    }
                    val quiet = runCatching { users.isQuietModeEnabled(user) }.getOrDefault(true)
                    profiles += AppProfile(serial, kind, quiet)
                    if (kind == ProfileKind.PRIVATE && (quiet || !runCatching { users.isUserUnlocked(user) }.getOrDefault(false))) return@forEach
                    runCatching { launcher.getActivityList(null, user) }.getOrElse { complete = false; emptyList() }.forEach { info ->
                        if (info.componentName.packageName != context.packageName) {
                            apps += LaunchableApp(
                                id = "$serial:${info.componentName.flattenToString()}",
                                packageName = info.componentName.packageName,
                                name = info.label.toString(),
                                profile = kind,
                                available = !quiet && runCatching { launcher.isActivityEnabled(info.componentName, user) }.getOrDefault(false),
                            )
                        }
                    }
                }
                mutable.value = CatalogState(apps.distinctBy { it.id }, profiles, true, complete)
            }
        }
    }

    fun launch(app: LaunchableApp): Boolean {
        val parts = app.id.split(':', limit = 2)
        val serial = parts.firstOrNull()?.toLongOrNull() ?: return false
        val user = users.getUserForSerialNumber(serial) ?: return false
        val component = parts.getOrNull(1)?.let(ComponentName::unflattenFromString) ?: return false
        return runCatching {
            check(!users.isQuietModeEnabled(user))
            check(users.isUserUnlocked(user))
            check(launcher.isActivityEnabled(component, user))
            launcher.startMainActivity(component, user, null, null)
        }.isSuccess
    }

    fun setProfileQuiet(serial: Long, quiet: Boolean): Boolean {
        val user = users.getUserForSerialNumber(serial) ?: return false
        if (Build.VERSION.SDK_INT < 28) return runCatching {
            context.startActivity(Intent(android.provider.Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (quiet) mutable.value = mutable.value.copy(
            apps = mutable.value.apps.filterNot { it.id.startsWith("$serial:") && it.profile == ProfileKind.PRIVATE },
            profiles = mutable.value.profiles.map { if (it.serial == serial) it.copy(quiet = true) else it },
        )
        return runCatching { users.requestQuietModeEnabled(quiet, user) }.getOrDefault(false).also { refresh() }
    }
}
