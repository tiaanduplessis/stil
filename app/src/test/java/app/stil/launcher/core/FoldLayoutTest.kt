package app.stil.launcher.core

import org.junit.Assert.*
import org.junit.Test

class FoldLayoutTest {
    private fun vertical(x: Float, width: Float = 0f) = Fold(Pane(x, 0f, x + width, 1000f), FoldAxis.VERTICAL, true)
    private fun horizontal(y: Float, height: Float = 0f) = Fold(Pane(0f, y, 1000f, y + height), FoldAxis.HORIZONTAL, true)
    private fun overlaps(a: Pane, b: Pane) = a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    @Test fun narrowOuterDisplayKeepsOneColumnAndBottomGestureArea() {
        val home = FoldLayout.home(320f, 800f, 1f, Alignment.LEFT, emptyList())
        assertEquals(HomeArrangement.STACKED, home.arrangement)
        assertTrue(home.clock.bottom <= home.apps.top)
        assertTrue(home.apps.bottom <= home.gestures.single().top)
        assertEquals(800f, home.gestures.single().bottom)
        assertEquals(56f, home.gestures.single().height)
    }

    @Test fun wideFlatWindowRespectsThePreferredSide() {
        val left = FoldLayout.home(840f, 700f, 1f, Alignment.LEFT, emptyList())
        val right = FoldLayout.home(840f, 700f, 1f, Alignment.RIGHT, emptyList())
        assertEquals(HomeArrangement.WIDE, left.arrangement)
        assertTrue(left.apps.right < left.clock.left)
        assertTrue(right.clock.right < right.apps.left)
    }

    @Test fun largeTextFallsBackWhenTwoReadablePanesWillNotFit() {
        val home = FoldLayout.home(700f, 700f, 2f, Alignment.LEFT, emptyList())
        assertEquals(HomeArrangement.STACKED, home.arrangement)
        assertTrue(home.apps.width > 600)
    }

    @Test fun bookHingeUsesItsActualBoundsAndLeavesBothBottomEdgesUsable() {
        val hinge = vertical(325f, 24f)
        val home = FoldLayout.home(720f, 800f, 1f, Alignment.LEFT, listOf(hinge))
        assertEquals(HomeArrangement.WIDE, home.arrangement)
        assertTrue(home.apps.right < hinge.bounds.left)
        assertTrue(home.clock.left > hinge.bounds.right)
        assertEquals(2, home.gestures.size)
        assertTrue(home.gestures.all { !overlaps(it, hinge.bounds) && it.bottom == 800f })
    }

    @Test fun tabletopKeepsClockAboveAndAppsBelowAnOffCentreFold() {
        val hinge = horizontal(460f, 10f)
        val home = FoldLayout.home(411f, 760f, 2f, Alignment.RIGHT, listOf(hinge))
        assertEquals(HomeArrangement.TABLETOP, home.arrangement)
        assertTrue(home.clock.bottom < hinge.bounds.top)
        assertTrue(home.apps.top > hinge.bounds.bottom)
        assertTrue(home.apps.height >= 48)
        assertEquals(760f, home.gestures.single().bottom)
    }

    @Test fun keyboardHidingTheLowerPaneMovesOtherScreensToTheUpperPane() {
        val panes = FoldLayout.safePanes(411f, 490f, listOf(horizontal(450f, 10f)))
        val page = FoldLayout.primaryPane(panes, Alignment.LEFT)
        assertEquals(0f, page.top)
        assertTrue(page.bottom < 450f)
    }

    @Test fun aFoldOutsideTheVisibleWindowDoesNotSplitIt() {
        assertEquals(listOf(Pane(0f, 0f, 360f, 300f)), FoldLayout.safePanes(360f, 300f, listOf(horizontal(500f))))
    }

    @Test fun flatNonOccludingCreaseDoesNotRemoveUsableContent() {
        assertEquals(1, FoldLayout.safePanes(700f, 800f, listOf(vertical(350f).copy(separates = false))).size)
    }

    @Test fun multipleHingesNeverLeaveContentOrGesturesInsideAnOcclusion() {
        val folds = listOf(vertical(400f, 20f), horizontal(450f, 16f))
        val panes = FoldLayout.safePanes(1000f, 900f, folds)
        assertEquals(4, panes.size)
        for (alignment in Alignment.entries) {
            val home = FoldLayout.home(1000f, 900f, 1f, alignment, folds)
            (listOf(home.clock, home.apps) + home.gestures).forEach { pane ->
                assertTrue(pane.left >= 0 && pane.top >= 0 && pane.right <= 1000 && pane.bottom <= 900)
                assertFalse(folds.any { overlaps(pane, it.bounds) })
            }
        }
    }

    @Test fun tinyAndTemporarilyEmptyWindowsDoNotProduceNegativeSizes() {
        for (width in listOf(0f, 160f, 320f)) for (height in listOf(0f, 160f, 240f)) {
            val home = FoldLayout.home(width, height, 2f, Alignment.LEFT, emptyList())
            (listOf(home.clock, home.apps) + home.gestures).forEach {
                assertTrue(it.width >= 0 && it.height >= 0)
                assertTrue(it.left >= 0 && it.top >= 0 && it.right <= width && it.bottom <= height)
            }
        }
    }
}
