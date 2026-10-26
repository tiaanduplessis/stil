package app.stil.launcher.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import app.stil.launcher.core.Alignment
import app.stil.launcher.core.Fold
import app.stil.launcher.core.FoldAxis
import app.stil.launcher.core.FoldLayout
import app.stil.launcher.core.HomePlacement
import app.stil.launcher.core.Pane
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

internal val LocalCompactHeight = staticCompositionLocalOf { false }

internal data class FoldWindow(val width: Float, val height: Float, val fontScale: Float, val folds: List<Fold>)

@Composable
internal fun FoldViewport(modifier: Modifier = Modifier, content: @Composable (FoldWindow) -> Unit) {
    val activity = LocalView.current.context as? Activity
    val changes = remember(activity) {
        activity?.let { owner -> WindowInfoTracker.getOrCreate(owner).windowLayoutInfo(owner)
            .map { info -> info.displayFeatures.filterIsInstance<FoldingFeature>() } } ?: flowOf(emptyList())
    }
    val features by changes.collectAsStateWithLifecycle(emptyList())
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    BoxWithConstraints(modifier.onGloballyPositioned { origin = it.positionInWindow() }.clipToBounds()) {
        val folds = features.map { feature ->
            val b = feature.bounds
            Fold(Pane((b.left - origin.x) / density.density, (b.top - origin.y) / density.density,
                (b.right - origin.x) / density.density, (b.bottom - origin.y) / density.density),
                if (feature.orientation == FoldingFeature.Orientation.HORIZONTAL) FoldAxis.HORIZONTAL else FoldAxis.VERTICAL,
                feature.isSeparating || feature.occlusionType == FoldingFeature.OcclusionType.FULL)
        }
        content(FoldWindow(maxWidth.value, maxHeight.value, density.fontScale, folds))
    }
}

/** The content stays in the same composition slot when a hinge or keyboard changes its bounds. */
@Composable
internal fun SafePane(window: FoldWindow, alignment: Alignment, content: @Composable (Modifier) -> Unit) {
    val pane = FoldLayout.primaryPane(FoldLayout.safePanes(window.width, window.height, window.folds), alignment)
    PositionedPanes(listOf(pane), Modifier.fillMaxSize().testTag("safe_pane_viewport")) {
        Box(Modifier.fillMaxSize().clipToBounds(), contentAlignment = UiAlignment.Center) {
            BoxWithConstraints(Modifier.fillMaxHeight().widthIn(max = 600.dp).fillMaxWidth()) {
                val compact = maxHeight.value / window.fontScale < 360
                CompositionLocalProvider(LocalCompactHeight provides compact) {
                    content(Modifier.fillMaxSize().padding(horizontal = if (maxWidth < 360.dp) 16.dp else 28.dp)
                        .padding(top = if (compact) 0.dp else 8.dp, bottom = if (compact) 8.dp else 16.dp))
                }
            }
        }
    }
}

@Composable
internal fun HomePanes(placement: HomePlacement, modifier: Modifier = Modifier, clock: @Composable () -> Unit,
    apps: @Composable () -> Unit, openApps: () -> Unit) {
    PositionedPanes(listOf(placement.clock, placement.apps) + placement.gestures,
        modifier.testTag("home_layout_${placement.arrangement.name}")) {
        Box(Modifier.fillMaxSize().testTag("home_clock")) { clock() }
        Box(Modifier.fillMaxSize().testTag("home_apps")) { apps() }
        placement.gestures.forEachIndexed { index, _ ->
            key(index) { Box(Modifier.fillMaxSize().testTag("home_gesture_$index")) {
                Spacer(Modifier.fillMaxSize().homeDrawerGesture(openApps))
            } }
        }
    }
}

@Composable
private fun PositionedPanes(bounds: List<Pane>, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { children, constraints ->
        val places = children.mapIndexed { index, child ->
            val pane = bounds[index]
            child.measure(Constraints.fixed((pane.width * density).roundToInt().coerceAtLeast(0),
                (pane.height * density).roundToInt().coerceAtLeast(0)))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            places.forEachIndexed { index, child ->
                child.place((bounds[index].left * density).roundToInt(), (bounds[index].top * density).roundToInt())
            }
        }
    }
}
