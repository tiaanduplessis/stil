package app.stil.launcher.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Bounds in dp, relative to the visible app content after system and keyboard insets. */
data class Pane(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = (right - left).coerceAtLeast(0f)
    val height get() = (bottom - top).coerceAtLeast(0f)
    val area get() = width * height

    fun inset(horizontal: Float, vertical: Float = 0f): Pane {
        val x = min(horizontal, width / 3)
        val y = min(vertical, height / 3)
        return Pane(left + x, top + y, right - x, bottom - y)
    }
}

enum class FoldAxis { HORIZONTAL, VERTICAL }
data class Fold(val bounds: Pane, val axis: FoldAxis, val separates: Boolean)
enum class HomeArrangement { STACKED, WIDE, TABLETOP }
data class HomePlacement(val arrangement: HomeArrangement, val clock: Pane, val apps: Pane, val gestures: List<Pane>)

object FoldLayout {
    private const val HINGE_MARGIN = 12f
    private const val GESTURE_HEIGHT = 96f

    fun safePanes(width: Float, height: Float, folds: List<Fold>): List<Pane> {
        var panes = listOf(Pane(0f, 0f, width, height))
        for (fold in folds.filter { it.separates }) {
            panes = panes.flatMap { pane ->
                val b = fold.bounds
                when {
                    fold.axis == FoldAxis.VERTICAL && b.top < pane.bottom && b.bottom > pane.top &&
                        b.left < pane.right && b.right > pane.left -> listOf(
                        pane.copy(right = max(pane.left, b.left - HINGE_MARGIN)),
                        pane.copy(left = min(pane.right, b.right + HINGE_MARGIN)),
                    )
                    fold.axis == FoldAxis.HORIZONTAL && b.left < pane.right && b.right > pane.left &&
                        b.top < pane.bottom && b.bottom > pane.top -> listOf(
                        pane.copy(bottom = max(pane.top, b.top - HINGE_MARGIN)),
                        pane.copy(top = min(pane.bottom, b.bottom + HINGE_MARGIN)),
                    )
                    else -> listOf(pane)
                }
            }.filter { it.width > 0 && it.height > 0 }
        }
        return panes.ifEmpty { listOf(Pane(0f, 0f, 0f, 0f)) }
    }

    fun primaryPane(panes: List<Pane>, alignment: Alignment): Pane {
        val usable = panes.filter { it.width >= 240 && it.height >= 160 }.ifEmpty { panes }
        val largest = usable.maxBy { it.area }
        val similar = usable.filter { it.area >= largest.area * 0.75f }
        return if (similar.all { abs(it.left - largest.left) < 1f }) similar.maxBy { it.top }
        else if (alignment == Alignment.LEFT) similar.minBy { it.left } else similar.maxBy { it.right }
    }

    fun home(width: Float, height: Float, fontScale: Float, alignment: Alignment, folds: List<Fold>): HomePlacement {
        val panes = safePanes(width, height, folds)
        val main = primaryPane(panes, alignment)
        val bottomPanes = panes.filter { abs(it.bottom - height) < 1f && it.width >= 48 && it.height >= GESTURE_HEIGHT }
        val gestures = bottomPanes.ifEmpty { listOf(main) }.map { it.copy(top = max(it.top, it.bottom - GESTURE_HEIGHT)) }
        fun appBounds(pane: Pane): Pane {
            val reserved = if (gestures.any { abs(it.bottom - pane.bottom) < 1f && it.left < pane.right && it.right > pane.left }) GESTURE_HEIGHT else 8f
            return pane.copy(bottom = max(pane.top, pane.bottom - reserved)).inset(if (pane.width < 360) 16f else 28f)
        }
        val lowerCandidates = panes.filter { it.top > 0 && it.width >= 160 && it.height >= 112 }
        val lowest = lowerCandidates.maxOfOrNull { it.bottom }
        val lower = lowerCandidates.filter { it.bottom == lowest }.takeIf { it.isNotEmpty() }?.let { primaryPane(it, alignment) }
        val upper = panes.filter { lower != null && it.bottom < lower.top && it.width >= 160 && it.height >= 64 &&
            it.left < lower.right && it.right > lower.left }.maxByOrNull { it.area }
        if (upper != null && lower != null && upper.bottom < lower.top && upper.left < lower.right && upper.right > lower.left) {
            return HomePlacement(HomeArrangement.TABLETOP, upper.inset(28f, 8f), appBounds(lower), gestures)
        }
        val requiredWidth = max(260f, 220f * fontScale + 56f)
        val left = panes.minBy { it.left }
        val right = panes.maxBy { it.right }
        val separatedPair = left.right < right.left && left.width >= requiredWidth && right.width >= requiredWidth &&
            min(left.height, right.height) >= 240
        val flatWide = panes.size == 1 && width >= max(600f, requiredWidth * 2 + 24) && height >= 240
        if (separatedPair || flatWide) {
            val fold = folds.firstOrNull { it.axis == FoldAxis.VERTICAL && it.bounds.left in requiredWidth..(width - requiredWidth) }
            val middle = fold?.bounds?.let { (it.left + it.right) / 2 } ?: width / 2
            val a = if (separatedPair) left else Pane(0f, 0f, middle - 12, height)
            val b = if (separatedPair) right else Pane(middle + 12, 0f, width, height)
            val apps = if (alignment == Alignment.LEFT) a else b
            val clock = if (alignment == Alignment.LEFT) b else a
            return HomePlacement(HomeArrangement.WIDE, clock.inset(28f, 24f), appBounds(apps), gestures)
        }
        val body = appBounds(main)
        val clockHeight = if (body.height >= 96) (body.height * 0.34f).coerceIn(48f, min(180f, body.height - 48)) else body.height / 2
        return HomePlacement(HomeArrangement.STACKED, body.copy(bottom = body.top + clockHeight),
            body.copy(top = body.top + clockHeight), gestures)
    }
}
