package app.stil.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature as testFold
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import app.stil.launcher.core.Alignment
import app.stil.launcher.core.FontChoice
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class FoldableInstrumentedTest {
    @get:Rule(order = 0) val folds = WindowLayoutInfoPublisherRule()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()
    private val graph get() = (rule.activity.application as StilApplication).graph
    private lateinit var firstId: String
    private lateinit var pins: List<String>
    private var originalFontScale = 1f
    private var densityToRestore: String? = null

    @Before fun configure() {
        originalFontScale = Settings.System.getFloat(rule.activity.contentResolver, Settings.System.FONT_SCALE, 1f)
        rule.waitUntil(15_000) { graph.catalog.state.value.apps.count { it.packageName == "app.stil.launcher.test" } >= 6 }
        val fixtures = graph.catalog.state.value.apps.filter { it.packageName == "app.stil.launcher.test" }.sortedBy { it.name }
        firstId = fixtures.first().id
        pins = fixtures.map { it.id }
        val labels = listOf("My phone", "Chat", "Maps", "Camera", "Music", "Read")
        runBlocking { graph.settings.update { LauncherSettings(onboarded = true, pins = pins, aliases = pins.zip(labels).toMap()) } }
        waitFor("home_surface")
    }

    @After fun restoreDevice() {
        shell("settings put system font_scale $originalFontScale")
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        folds.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        densityToRestore?.let { shell("wm density $it") }
    }

    private fun shell(command: String): String =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }

    private fun waitFor(tag: String) {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        rule.waitForIdle()
    }

    private fun publish(orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.HORIZONTAL,
        fraction: Float = 0.5f, size: Int = 0): FoldingFeature {
        rule.waitForIdle()
        val display = rule.activity.window.decorView
        val center = ((if (orientation == FoldingFeature.Orientation.HORIZONTAL) display.height else display.width) * fraction).toInt()
        val feature = testFold(rule.activity, center = center, size = size, orientation = orientation)
        folds.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(feature)))
        rule.waitForIdle()
        return feature
    }

    private fun settings() {
        rule.onNodeWithTag("home_surface").performTouchInput { longClick(Offset(width * 0.9f, height * 0.4f)) }
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }

    private fun apps() {
        rule.onNodeWithTag("home_gesture_0").performTouchInput {
            swipe(Offset(centerX, bottom - 8f), Offset(centerX, top - 220f), 300)
        }
        rule.onNodeWithTag("app_search").assertIsDisplayed()
    }

    @Test fun tabletopUsesRealFoldBoundsAndKeepsAppsAndGesturesBelowIt() {
        // Equal panes: below a 75% area ratio All apps opens in the larger upper pane, as on a 720x1600 phone at 2.75 density.
        val feature = publish(fraction = 0.5f, size = 12)
        waitFor("home_layout_TABLETOP")
        val density = rule.activity.resources.displayMetrics.density
        val clockBottom = rule.onNodeWithTag("home_clock").getUnclippedBoundsInRoot().bottom.value
        val appsTop = rule.onNodeWithTag("home_apps").getUnclippedBoundsInRoot().top.value
        val foldTop = feature.bounds.top / density
        val foldBottom = feature.bounds.bottom / density
        assertTrue("clock bottom $clockBottom, fold top $foldTop (density $density)", clockBottom < foldTop)
        assertTrue("apps top $appsTop, fold bottom $foldBottom (density $density)", appsTop > foldBottom)
        rule.onNodeWithTag("home_$firstId").assertIsDisplayed()
        apps()
        val search = rule.onNodeWithTag("app_search").getUnclippedBoundsInRoot()
        assertTrue("search top ${search.top.value}, fold bottom $foldBottom (density $density)", search.top.value > foldBottom)
    }

    @Test fun bookPostureKeepsBothBottomGesturesAndMirrorsThePinnedPane() {
        // This scenario requires two readable panes; the CI phone defaults to only 582x214dp in landscape.
        if (rule.activity.resources.displayMetrics.densityDpi > 240) {
            densityToRestore = Regex("Override density: (\\d+)").find(shell("wm density"))?.groupValues?.get(1) ?: "reset"
            shell("wm density 240")
            rule.waitUntil(10_000) { rule.activity.resources.displayMetrics.densityDpi == 240 }
            waitFor("home_surface")
        }
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        waitFor("home_layout_WIDE")
        val feature = publish(FoldingFeature.Orientation.VERTICAL, fraction = 0.48f, size = 16)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("home_gesture_1").fetchSemanticsNodes().isNotEmpty() }
        val density = rule.activity.resources.displayMetrics.density
        assertTrue(rule.onNodeWithTag("home_apps").getUnclippedBoundsInRoot().right.value < feature.bounds.left / density)
        runBlocking { graph.settings.update { it.copy(alignment = Alignment.RIGHT) } }
        rule.waitForIdle()
        assertTrue(rule.onNodeWithTag("home_apps").getUnclippedBoundsInRoot().left.value > feature.bounds.right / density)
        rule.onNodeWithTag("home_gesture_1").performTouchInput {
            swipe(Offset(centerX, bottom - 8f), Offset(centerX, top - 220f), 300)
        }
        rule.onNodeWithTag("app_search").assertIsDisplayed()
    }

    @Test fun searchTextAndFocusSurvivePostureChangesAndActivityRecreation() {
        apps()
        rule.onNodeWithTag("app_search").performClick().performTextInput("Stil fixture")
        rule.onNodeWithTag("app_search").performTextInputSelection(androidx.compose.ui.text.TextRange(2))
        publish(fraction = 0.5f)
        rule.onNodeWithTag("app_search").assertTextContains("Stil fixture").assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, androidx.compose.ui.text.TextRange(2)))
        rule.activityRule.scenario.recreate()
        waitFor("app_search")
        publish(fraction = 0.6f)
        rule.onNodeWithTag("app_search").assertTextContains("Stil fixture").assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, androidx.compose.ui.text.TextRange(2)))
        rule.waitUntil(10_000) { runCatching { rule.onNodeWithTag("app_list").performScrollToNode(hasText("My phone")) }.isSuccess }
        rule.onNode(hasText("My phone") and hasAnyAncestor(hasTestTag("app_list"))).assertIsDisplayed()
    }

    @Test fun unsavedRenameAndFocusSurviveARecreatedActivity() {
        rule.onNodeWithTag("home_$firstId").performTouchInput { longClick() }
        rule.onNodeWithTag("rename_input").performClick().performTextReplacement("Still editing")
        rule.onNodeWithTag("rename_input").performTextInputSelection(androidx.compose.ui.text.TextRange(3, 7))
        publish(fraction = 0.5f)
        rule.onNodeWithTag("rename_input").assertTextContains("Still editing").assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, androidx.compose.ui.text.TextRange(3, 7)))
        rule.activityRule.scenario.recreate()
        waitFor("rename_input")
        publish(fraction = 0.6f)
        rule.onNodeWithTag("rename_input").assertTextContains("Still editing").assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, androidx.compose.ui.text.TextRange(3, 7)))
        assertEquals("My phone", runBlocking { graph.settings.current().aliases[firstId] })
        rule.onNodeWithText("Save").performScrollTo().performClick()
        rule.waitUntil(5_000) { runBlocking { graph.settings.current().aliases[firstId] == "Still editing" } }
    }

    @Test fun settingsScrollOffsetSurvivesAChangeToAShorterPane() {
        publish(fraction = 0.5f)
        waitFor("home_layout_TABLETOP")
        settings()
        rule.onNodeWithText("Default home app").performScrollTo()
        fun range() = rule.onNodeWithTag("settings_list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        val before = range().value()
        assertTrue(before > 0)
        publish(fraction = 0.55f)
        rule.waitForIdle()
        // A pane that crosses the compact-height threshold (a bottom pane under a navigation bar) shrinks the content, so the offset may clamp to the new maximum.
        range().let { after -> assertEquals(minOf(before, after.maxValue()), after.value(), 1f) }
        rule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test fun sixPinsAtLargeTypeRemainScrollableWithAnAccessibleBottomGesture() {
        shell("settings put system font_scale 2.0")
        rule.waitUntil(10_000) { rule.activity.resources.configuration.fontScale >= 1.9f }
        waitFor("home_surface")
        runBlocking { graph.settings.update { it.copy(font = FontChoice.MONO, textSize = 36, theme = ThemeMode.BLACK) } }
        publish(fraction = 0.5f)
        waitFor("home_layout_TABLETOP")
        rule.onNodeWithTag("home_${pins.last()}").performScrollTo().assertIsDisplayed()
        val gesture = rule.onNodeWithTag("home_gesture_0").getUnclippedBoundsInRoot()
        assertTrue((gesture.bottom - gesture.top).value >= 48)
        val last = rule.onNodeWithTag("home_${pins.last()}").getUnclippedBoundsInRoot()
        assertTrue("Last pin $last must end above gesture $gesture", last.bottom.value <= gesture.top.value + 1f / rule.activity.resources.displayMetrics.density)
        apps()
        rule.onNodeWithText("Home", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Edit", substring = false).assertIsDisplayed()
        rule.onNodeWithTag("app_search").assertIsDisplayed()
    }

    @Test fun feedbackStaysInsideASafePane() {
        val feature = publish(FoldingFeature.Orientation.VERTICAL, fraction = 0.48f, size = 16)
        val model = ViewModelProvider(rule.activity)[LauncherViewModel::class.java]
        runBlocking { model.messages.emit(R.string.home_full) }
        val message = rule.activity.getString(R.string.home_full)
        rule.waitUntil(5_000) { rule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
        val bounds = rule.onNodeWithText(message).getUnclippedBoundsInRoot()
        assertTrue(bounds.right.value < feature.bounds.left / rule.activity.resources.displayMetrics.density)
    }

    @Test fun foldingDoesNotRestartOrConfirmTheLaunchPause() {
        val launched = AtomicReference<String?>(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { launched.set(intent.getStringExtra("activity")) }
        }
        val receiverContext = rule.activity.applicationContext
        ContextCompat.registerReceiver(receiverContext, receiver, IntentFilter("app.stil.launcher.test.FIXTURE_OPENED"), ContextCompat.RECEIVER_EXPORTED)
        try {
            runBlocking { graph.settings.update { it.copy(paused = setOf(firstId)) } }
            rule.waitForIdle()
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.onNodeWithTag("confirm_open").assertIsNotEnabled()
            val model = ViewModelProvider(rule.activity)[LauncherViewModel::class.java]
            rule.waitUntil(4_000) { model.remaining == 1 }
            publish(fraction = 0.55f)
            assertTrue("Posture changes must not reset the countdown", model.remaining <= 1)
            rule.activityRule.scenario.recreate()
            waitFor("confirm_open")
            assertSame(model, ViewModelProvider(rule.activity)[LauncherViewModel::class.java])
            publish(fraction = 0.5f)
            assertTrue("Recreation must not reset the countdown", model.remaining <= 1)
            rule.waitUntil(1_500) { rule.onAllNodesWithText("Ready").fetchSemanticsNodes().isNotEmpty() }
            assertNull(launched.get())
            rule.onNodeWithTag("confirm_open").assertIsDisplayed().assertIsEnabled().performClick()
            rule.waitUntil(15_000) { launched.get() != null }
            assertEquals(FixtureActivity::class.java.name, launched.get())
        } finally { receiverContext.unregisterReceiver(receiver) }
    }
}
