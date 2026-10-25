package app.stil.launcher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import app.stil.launcher.core.FontChoice
import app.stil.launcher.core.ThemeMode
import androidx.compose.ui.graphics.toPixelMap
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.WallpaperMode
import app.stil.launcher.data.DailyWallpaperWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class LauncherInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val graph get() = (rule.activity.application as StilApplication).graph
    private lateinit var firstId: String
    private lateinit var secondId: String

    @Before fun configureOwnApp() {
        rule.waitUntil(15_000) { graph.catalog.state.value.apps.count { it.packageName == "app.stil.launcher.test" } >= 2 }
        val fixtures = graph.catalog.state.value.apps.filter { it.packageName == "app.stil.launcher.test" }.sortedBy { it.name }
        firstId = fixtures[0].id
        secondId = fixtures[1].id
        runBlocking { graph.settings.update { LauncherSettings(onboarded = true, pins = listOf(firstId, secondId)) } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun setupWritesTheChosenHomeApps() {
        runBlocking { graph.settings.update { LauncherSettings() } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Choose home apps").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("finish_setup").assertIsEnabled().performClick()
        rule.onNodeWithText("Use Stil as home?").assertIsDisplayed()
        val saved = runBlocking { graph.settings.current() }
        assertTrue(saved.onboarded)
        assertTrue(saved.pins.size in 1..6)
        rule.onNodeWithText("Not now").performClick()
        rule.onNodeWithText("All apps").assertIsDisplayed()
    }

    @Test fun fontSelectionPersistsThroughActivityRecreation() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Font").performClick()
        rule.onNodeWithTag("font_SERIF").performClick()
        rule.waitUntil(5_000) { runBlocking { graph.settings.current().font == FontChoice.SERIF } }
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Back").performClick()
        rule.activityRule.scenario.recreate()
        assertEquals(FontChoice.SERIF, runBlocking { graph.settings.current().font })
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Serif").assertIsDisplayed()
    }

    @Test fun blackThemeRendersReadableSettingsText() {
        runBlocking { graph.settings.update { it.copy(theme = ThemeMode.BLACK) } }
        rule.waitForIdle()
        rule.onNodeWithText("Settings").performClick()
        val pixels = rule.onNodeWithText("Settings").captureToImage().toPixelMap()
        var lightPixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val color = pixels[x, y]
            if (color.red + color.green + color.blue > 1.5f) lightPixels++
        }
        assertTrue("Settings text must be visible on the black background", lightPixels > pixels.width * pixels.height / 100)
    }

    @Test fun renameIsSearchableByAliasAndOriginalName() {
        rule.onNodeWithTag("home_$firstId").performTouchInput { longClick() }
        rule.onNodeWithTag("rename_input").performTextReplacement("Go places")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { runBlocking { graph.settings.current().aliases[firstId] == "Go places" } }
        repeat(3) { rule.onNodeWithText("Back").performClick() }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("All apps").performClick()
        rule.onNodeWithTag("app_search").performTextInput("Go places")
        rule.waitUntil(5_000) { rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).isDisplayed() }
        rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).assertIsDisplayed()
        rule.onNodeWithText("All apps").assertIsDisplayed()
        rule.onNodeWithTag("app_search").performTextReplacement("Stil fixture")
        rule.waitUntil(5_000) { rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).isDisplayed() }
        rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).assertIsDisplayed()
    }

    @Test fun hiddenAppCanBeRecovered() {
        rule.onNodeWithTag("home_$firstId").performTouchInput { longClick() }
        rule.onNodeWithText("Hide from Stil").performScrollTo().performClick()
        rule.waitUntil(5_000) { runBlocking { firstId in graph.settings.current().hidden } }
        repeat(3) { rule.onNodeWithText("Back").performClick() }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("All apps").performClick()
        rule.onNodeWithTag("app_search").performTextInput("Stil fixture")
        rule.onNode(hasText("Stil fixture", substring = false) and hasAnyAncestor(hasTestTag("app_list"))).assertDoesNotExist()
        rule.onNodeWithText("Home", substring = false).performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Hidden apps").performScrollTo().performClick()
        rule.onNodeWithText("Stil fixture", substring = false).performClick()
        rule.waitUntil(5_000) { runBlocking { firstId !in graph.settings.current().hidden } }
    }

    @Test fun cancelPauseNeverLaunchesTheApp() {
        observeLaunch { launched ->
            runBlocking { graph.settings.update { it.copy(paused = setOf(firstId)) } }
            rule.waitForIdle()
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.onNodeWithTag("confirm_open").assertIsNotEnabled()
            rule.onNodeWithText("Cancel").performClick()
            val deadline = SystemClock.elapsedRealtime() + 3_200
            rule.waitUntil(5_000) { SystemClock.elapsedRealtime() >= deadline }
            rule.onNodeWithText("Settings").assertIsDisplayed()
            assertNull(launched.get())
        }
    }

    @Test fun pauseRequiresThreeSecondsAndExplicitConfirmationThenLaunchesRealActivity() {
        observeLaunch { launched ->
            runBlocking { graph.settings.update { it.copy(paused = setOf(firstId)) } }
            rule.waitForIdle()
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.onNodeWithTag("confirm_open").assertIsNotEnabled()
            rule.waitUntil(6_000) { rule.onAllNodesWithText("Ready").fetchSemanticsNodes().isNotEmpty() }
            assertNull(launched.get())
            rule.onNodeWithTag("confirm_open").performClick()
            rule.waitUntil(5_000) { launched.get() != null }
            assertEquals(FixtureActivity::class.java.name, launched.get())
            rule.activity.runOnUiThread {
                rule.activity.startActivity(Intent(rule.activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            }
        }
    }

    @Test fun disabledWallpaperWorkerDoesNotMakeChanges() = runBlocking {
        val before = graph.settings.current()
        val worker = TestListenableWorkerBuilder<DailyWallpaperWorker>(rule.activity.applicationContext).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(before, graph.settings.current())
        assertEquals(WallpaperMode.OFF, graph.settings.current().wallpaper)
    }

    private fun observeLaunch(block: (AtomicReference<String?>) -> Unit) {
        val launched = AtomicReference<String?>(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { launched.set(intent.getStringExtra("activity")) }
        }
        ContextCompat.registerReceiver(rule.activity, receiver, IntentFilter("app.stil.launcher.test.FIXTURE_OPENED"), ContextCompat.RECEIVER_EXPORTED)
        try { block(launched) } finally { rule.activity.unregisterReceiver(receiver) }
    }
}
