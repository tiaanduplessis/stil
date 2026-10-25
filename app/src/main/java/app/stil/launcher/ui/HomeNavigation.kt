package app.stil.launcher.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stil.launcher.R
import kotlin.math.abs

@Composable
internal fun Modifier.homeNavigation(onAllApps: () -> Unit, onSettings: () -> Unit): Modifier {
    val openApps by rememberUpdatedState(onAllApps)
    val openSettings by rememberUpdatedState(onSettings)
    val haptics = LocalHapticFeedback.current
    val home = stringResource(R.string.home)
    val apps = stringResource(R.string.all_apps)
    val settings = stringResource(R.string.settings)
    return this.testTag("home_surface")
        .semantics {
            contentDescription = home
            customActions = listOf(
                CustomAccessibilityAction(apps) { openApps(); true },
                CustomAccessibilityAction(settings) { openSettings(); true },
            )
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) false
            else when (event.key) {
                Key.Enter, Key.NumPadEnter, Key.Spacebar -> { openApps(); true }
                Key.Menu -> { openSettings(); true }
                else -> false
            }
        }
        .focusable()
        .pointerInput(Unit) {
            // Child app/clock controls consume their own presses before this blank-space handler.
            detectTapGestures(onLongPress = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                openSettings()
            })
        }
}

@Composable
internal fun Modifier.homeDrawerGesture(onAllApps: () -> Unit): Modifier {
    val openApps by rememberUpdatedState(onAllApps)
    val threshold = with(LocalDensity.current) { 36.dp.toPx() }
    return this.testTag("home_drawer_gesture").pointerInput(threshold) {
        var movement = Offset.Zero
        detectDragGestures(
            onDragStart = { movement = Offset.Zero },
            onDrag = { change, amount -> movement += amount; change.consume() },
            onDragCancel = { movement = Offset.Zero },
            onDragEnd = {
                if (movement.y <= -threshold && -movement.y > abs(movement.x)) openApps()
            },
        )
    }
}
