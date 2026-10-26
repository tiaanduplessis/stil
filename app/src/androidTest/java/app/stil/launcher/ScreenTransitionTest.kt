package app.stil.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stil.launcher.ui.ScreenTransition
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenTransitionTest {
    private var durationScale = 1f
    @get:Rule val rule = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float get() = durationScale
    })
    private var screen by mutableStateOf(Screen.HOME)
    private var clicks = 0

    private fun showScreens() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ScreenTransition(screen) { destination, motion ->
                Column(Modifier.fillMaxSize().then(motion).testTag("destination")) {
                    Text(destination.name)
                    Button(onClick = { clicks++ }) { Text("Act") }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun left() = rule.onNodeWithTag("destination").getUnclippedBoundsInRoot().left.value
    private fun top() = rule.onNodeWithTag("destination").getUnclippedBoundsInRoot().top.value

    @Test fun appsSlideUpFromTheBottomAndRemoveHomeImmediately() {
        showScreens()
        val bounds = rule.onNodeWithTag("destination").getUnclippedBoundsInRoot()
        val height = (bounds.bottom - bounds.top).value
        rule.runOnIdle { screen = Screen.APPS }
        rule.mainClock.advanceTimeByFrame()
        val start = top()
        assertTrue("Drawer should enter from below: $start / $height", start > height * 0.8f && start <= height)
        assertEquals(0f, left(), 0.1f)
        rule.onNodeWithText("HOME").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(120)
        assertTrue(top() > 0f && top() < start)
        rule.mainClock.advanceTimeBy(200)
        assertEquals(0f, top(), 0.1f)
        rule.onNodeWithText("APPS").assertIsDisplayed()
    }

    @Test fun interruptedDrawerEntranceReturnsHomeWithoutLeavingSearchContent() {
        showScreens()
        rule.runOnIdle { screen = Screen.APPS }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { screen = Screen.HOME }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("APPS").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(240)
        assertEquals(0f, top(), 0.1f)
        assertEquals(0f, left(), 0.1f)
        rule.onNodeWithText("HOME").assertIsDisplayed()
    }

    @Test fun disabledMotionSkipsTheDrawerSlide() {
        durationScale = 0f
        showScreens()
        rule.runOnIdle { screen = Screen.APPS }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0f, top(), 0.1f)
        rule.onNodeWithText("APPS").assertIsDisplayed()
        rule.onNodeWithText("Act").performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun navigationMovesBrieflyWithoutRetainingOldContentOrBlockingTaps() {
        showScreens()
        assertEquals(0f, left(), 0.1f)
        rule.runOnIdle { screen = Screen.SETTINGS }
        rule.mainClock.advanceTimeByFrame()
        val start = left()
        assertTrue("Navigation should have a small entrance offset", start > 0f && start <= 8f)
        rule.onNodeWithText("HOME").assertDoesNotExist()
        rule.onNodeWithText("SETTINGS").assertIsDisplayed()
        rule.onNodeWithText("Act").performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
        rule.mainClock.advanceTimeBy(64)
        assertTrue(left() < start)
        rule.mainClock.advanceTimeBy(240)
        assertEquals(0f, left(), 0.1f)
    }

    @Test fun rapidNavigationKeepsOnlyTheLatestDestination() {
        showScreens()
        rule.runOnIdle { screen = Screen.SETTINGS }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { screen = Screen.APPS }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("SETTINGS").assertDoesNotExist()
        rule.onNodeWithText("HOME").assertDoesNotExist()
        rule.onNodeWithText("APPS").assertExists()
        rule.mainClock.advanceTimeBy(240)
        rule.onNodeWithText("APPS").assertIsDisplayed()
        assertEquals(0f, left(), 0.1f)
    }

    @Test fun disabledMotionSettlesOnTheNextAnimationFrame() {
        durationScale = 0f
        showScreens()
        rule.runOnIdle { screen = Screen.SETTINGS }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0f, left(), 0.1f)
        rule.onNodeWithText("HOME").assertDoesNotExist()
        rule.onNodeWithText("Act").performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun launchConfirmationAndCancellationHaveNoEntranceDelay() {
        showScreens()
        rule.runOnIdle { screen = Screen.SETTINGS }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { screen = Screen.PAUSE }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0f, left(), 0.1f)
        rule.onNodeWithText("PAUSE").assertIsDisplayed()
        rule.runOnIdle { screen = Screen.HOME }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0f, left(), 0.1f)
        rule.onNodeWithText("PAUSE").assertDoesNotExist()
    }
}
