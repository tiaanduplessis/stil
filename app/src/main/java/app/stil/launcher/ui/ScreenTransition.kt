package app.stil.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.stil.launcher.Screen

@Composable
internal fun ScreenTransition(screen: Screen, content: @Composable (Screen, Modifier) -> Unit) {
    var previous by remember { mutableStateOf(screen) }
    val progress = remember(screen) {
        val immediate = screen == previous || screen == Screen.PAUSE || previous == Screen.PAUSE
        Animatable(if (immediate) 1f else 0f)
    }
    val direction = remember(screen) { if (screen.depth < previous.depth) -1f else 1f }
    val layoutDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    SideEffect { previous = screen }
    val drawer = screen == Screen.APPS
    LaunchedEffect(progress) { progress.animateTo(1f, tween(if (drawer) 240 else 160, easing = FastOutSlowInEasing)) }
    val motion = Modifier.graphicsLayer {
        if (drawer) translationY = size.height * (1f - progress.value)
        else {
            alpha = 0.92f + 0.08f * progress.value
            translationX = 8.dp.toPx() * (1f - progress.value) * direction * layoutDirection
        }
    }
    // Dispose the old screen immediately, including its input and private app content.
    key(screen) { content(screen, motion) }
}

private val Screen.depth: Int
    get() = when (this) {
        Screen.HOME, Screen.SETUP, Screen.DEFAULT, Screen.PAUSE -> 0
        Screen.APPS, Screen.SETTINGS -> 1
        Screen.FONT, Screen.WALLPAPER, Screen.MANAGE, Screen.HIDDEN -> 2
        Screen.EDIT -> 3
    }
