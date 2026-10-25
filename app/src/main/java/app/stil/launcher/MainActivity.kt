package app.stil.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import app.stil.launcher.ui.StilApp

class MainActivity : ComponentActivity() {
    private val model: LauncherViewModel by viewModels()
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { model.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StilApp(model, ::chooseHome, ::openClock) }
    }

    override fun onResume() { super.onResume(); model.refresh() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) model.navigate(Screen.HOME)
    }

    private fun chooseHome() {
        val intent = if (Build.VERSION.SDK_INT >= 29) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_HOME) && !manager.isRoleHeld(RoleManager.ROLE_HOME))
                manager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            else Intent(Settings.ACTION_HOME_SETTINGS)
        } else Intent(Settings.ACTION_HOME_SETTINGS)
        runCatching { roleRequest.launch(intent) }.onFailure { model.messages.tryEmit(R.string.settings_unavailable) }
    }

    private fun openClock() {
        runCatching { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
            .recoverCatching { startActivity(Intent(Settings.ACTION_DATE_SETTINGS)) }
            .onFailure { model.messages.tryEmit(R.string.app_unavailable) }
    }
}
