package app.stil.launcher

import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
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
import app.stil.launcher.data.launcherDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
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
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("home_surface").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun setupWritesTheChosenHomeApps() {
        runBlocking { graph.settings.update { LauncherSettings() } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Choose home apps").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("finish_setup").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onNodeWithText("Use Stil as home?").isDisplayed() }
        rule.onNodeWithText("Use Stil as home?").assertIsDisplayed()
        val saved = runBlocking { graph.settings.current() }
        assertTrue(saved.onboarded)
        assertTrue(saved.pins.size in 1..6)
        rule.onNodeWithText("Not now").performClick()
        rule.onNodeWithTag("home_surface").assertIsDisplayed()
    }

    @Test fun fontSelectionPersistsThroughActivityRecreation() {
        openSettings()
        rule.onNodeWithText("Font").performClick()
        rule.onNodeWithTag("font_SERIF").performClick()
        rule.waitUntil(5_000) { runBlocking { graph.settings.current().font == FontChoice.SERIF } }
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Back").performClick()
        rule.activityRule.scenario.recreate()
        assertEquals(FontChoice.SERIF, runBlocking { graph.settings.current().font })
        openSettings()
        rule.onNodeWithText("Serif").assertIsDisplayed()
    }

    @Test fun blackThemeRendersReadableSettingsText() {
        runBlocking { graph.settings.update { it.copy(theme = ThemeMode.BLACK) } }
        rule.waitForIdle()
        openSettings()
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
        openApps()
        rule.onNodeWithTag("app_search").performTextInput("Stil fixture")
        rule.waitUntil(5_000) { rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).isDisplayed() }
        rule.onNode(hasText("Go places") and hasAnyAncestor(hasTestTag("app_list"))).assertIsDisplayed()
        rule.onNodeWithText("All apps").assertIsDisplayed()
        observeLaunch { launched ->
            rule.onNodeWithTag("app_search").performTextReplacement("Go places")
            rule.waitUntil(15_000) { launched.get() != null }
            assertEquals(FixtureActivity::class.java.name, launched.get())
            returnToStil()
        }
    }

    @Test fun hiddenAppCanBeRecovered() {
        rule.onNodeWithTag("home_$firstId").performTouchInput { longClick() }
        rule.onNodeWithText("Hide from Stil").performScrollTo().performClick()
        rule.waitUntil(5_000) { runBlocking { firstId in graph.settings.current().hidden } }
        repeat(3) { rule.onNodeWithText("Back").performClick() }
        rule.activityRule.scenario.recreate()
        openApps()
        rule.onNodeWithTag("app_search").performTextInput("Stil fixture")
        rule.onNode(hasText("Stil fixture", substring = false) and hasAnyAncestor(hasTestTag("app_list"))).assertDoesNotExist()
        rule.onNodeWithText("Home", substring = false).performClick()
        rule.activityRule.scenario.recreate()
        openSettings()
        rule.onNodeWithText("Hidden apps").performScrollTo().performClick()
        rule.onNodeWithText("Stil fixture", substring = false).performClick()
        rule.waitUntil(5_000) { runBlocking { firstId !in graph.settings.current().hidden } }
    }

    @Test fun narrowingSearchToOneMatchLaunchesAndReopeningStartsWithAnEmptyQuery() {
        observeLaunch { launched ->
            openApps()
            rule.onNodeWithTag("app_search").performTextInput("Stil fixture")
            rule.onNodeWithTag("app_search").assertIsDisplayed()
            assertNull(launched.get())
            rule.onNodeWithTag("app_search").performTextInput(" extra")
            rule.waitUntil(15_000) { launched.get() != null }
            assertEquals(SecondFixtureActivity::class.java.name, launched.get())
            returnToStil()
            rule.onNodeWithTag("home_surface").assertIsDisplayed()
            openApps()
            rule.onNodeWithTag("app_search").assertTextEquals("Search apps", "")
        }
    }

    @Test fun emptyMissingAndMultipleSearchMatchesStayInTheDrawer() {
        observeLaunch { launched ->
            openApps()
            rule.onNodeWithTag("app_search").performTextInput("no app matches this query")
            rule.onNodeWithText("No matches").assertIsDisplayed()
            rule.onNodeWithText("Clear search").performClick()
            rule.onNodeWithTag("app_search").performTextInput("   ")
            rule.onNodeWithTag("app_search").assertIsDisplayed()
            rule.onNodeWithTag("app_search").performTextReplacement("Stil fixture")
            rule.onNodeWithTag("app_search").assertIsDisplayed()
            assertNull(launched.get())
        }
    }

    @Test fun resultChangesRestorationAndCursorMovementCannotAutoLaunch() {
        observeLaunch { launched ->
            openApps()
            rule.onNodeWithTag("app_search").performClick().performTextInput("Unique alias")
            rule.onNodeWithText("No matches").assertIsDisplayed()
            runBlocking { graph.settings.update { it.copy(aliases = mapOf(firstId to "Unique alias")) } }
            rule.waitUntil(5_000) { rule.onNode(hasText("Unique alias") and hasAnyAncestor(hasTestTag("app_list"))).isDisplayed() }
            rule.activityRule.scenario.recreate()
            rule.onNodeWithTag("app_search").assertTextContains("Unique alias").assertIsFocused()
            rule.onNodeWithTag("app_search").performTextInputSelection(androidx.compose.ui.text.TextRange(2))
            rule.onNodeWithTag("app_search").assertIsDisplayed()
            assertNull(launched.get())
        }
    }

    @Test fun automaticMatchKeepsThePauseAndCancelDoesNotLaunch() {
        observeLaunch { launched ->
            runBlocking { graph.settings.update { it.copy(paused = setOf(secondId)) } }
            rule.waitForIdle()
            openApps()
            rule.onNodeWithTag("app_search").performTextInput("Stil fixture extra")
            rule.onNodeWithTag("confirm_open").assertIsNotEnabled()
            rule.waitUntil(6_000) { rule.onAllNodesWithText("Ready").fetchSemanticsNodes().isNotEmpty() }
            assertNull(launched.get())
            rule.onNodeWithText("Cancel").performClick()
            rule.onNodeWithTag("app_search").assertIsDisplayed()
            assertNull(launched.get())
        }
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
            rule.onNodeWithTag("home_surface").assertIsDisplayed()
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

    @Test fun actualLaunchesCountOnceWhileCancellationAndFailedLaunchDoNot() {
        runBlocking { rule.activity.launcherDataStore.edit { it.remove(stringPreferencesKey("launch_stats_v1")) } }
        observeLaunch { launched ->
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.waitUntil(15_000) { launched.get() != null }
            returnToStil()
            rule.waitUntil(5_000) { runBlocking { graph.stats.history.first().days[LocalDate.now().toString()]?.get(firstId) == 1 } }
            launched.set(null)
            openApps()
            rule.onNodeWithTag("app_search").performTextInput("Stil fixture extra")
            rule.waitUntil(15_000) { launched.get() != null }
            returnToStil()
            runBlocking { graph.settings.update { it.copy(paused = setOf(firstId)) } }
            rule.waitForIdle()
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.onNodeWithText("Cancel").performClick()
            val model = ViewModelProvider(rule.activity)[LauncherViewModel::class.java]
            rule.runOnIdle { model.open(graph.catalog.state.value.apps.first { it.id == firstId }.copy(id = "0:missing.pkg/None")) }
            rule.waitForIdle()
            assertEquals(mapOf(firstId to 1, secondId to 1), runBlocking { graph.stats.history.first().days[LocalDate.now().toString()] })
            rule.waitUntil(8_000) { rule.onAllNodesWithText(rule.activity.getString(R.string.app_unavailable)).fetchSemanticsNodes().isEmpty() }
            launched.set(null)
            rule.onNodeWithTag("home_$firstId").performClick()
            rule.waitUntil(6_000) { rule.onAllNodesWithText("Ready").fetchSemanticsNodes().isNotEmpty() }
            assertNull(launched.get())
            rule.onNodeWithTag("confirm_open").performClick()
            rule.waitUntil(15_000) { launched.get() != null }
            returnToStil()
            rule.waitUntil(5_000) { runBlocking { graph.stats.history.first().days[LocalDate.now().toString()]?.get(firstId) == 2 } }
        }
    }

    @Test fun statsPeriodsShowPersistedCountsAndExcludeHiddenApps() {
        runBlocking {
            rule.activity.launcherDataStore.edit { it.remove(stringPreferencesKey("launch_stats_v1")) }
            val first = graph.catalog.state.value.apps.first { it.id == firstId }
            val second = graph.catalog.state.value.apps.first { it.id == secondId }
            repeat(2) { graph.stats.record(first, LocalDate.now().minusDays(29)) }
            repeat(3) { graph.stats.record(second, LocalDate.now().minusDays(6)) }
            graph.stats.record(first)
        }
        openSettings()
        rule.onNodeWithText("Launch stats").performClick()
        rule.onNodeWithTag("stats_total", useUnmergedTree = true).assertTextEquals("4")
        rule.onNodeWithText("Today").performClick()
        rule.onNodeWithTag("stats_total", useUnmergedTree = true).assertTextEquals("1")
        rule.onNodeWithText("30 days").performClick()
        rule.onNodeWithTag("stats_total", useUnmergedTree = true).assertTextEquals("6")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("stats_total", useUnmergedTree = true).assertTextEquals("6")
        runBlocking { graph.settings.update { it.copy(hidden = setOf(secondId)) } }
        rule.waitUntil(5_000) { rule.onNodeWithTag("stats_total", useUnmergedTree = true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text == "3" }
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }

    private fun openSettings() {
        rule.onNodeWithTag("home_surface").performTouchInput {
            longClick(Offset(centerX, height * 0.4f))
        }
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }

    private fun openApps() {
        rule.onNodeWithTag("home_surface").performTouchInput {
            swipe(Offset(centerX, bottom - 8f), Offset(centerX, bottom - 300f), 300)
        }
        rule.onNodeWithTag("app_search").assertIsDisplayed()
    }

    @Test fun homeIsQuietAndSwipeFromBottomOpensApps() {
        rule.onNodeWithText("All apps").assertDoesNotExist()
        rule.onNodeWithText("Settings").assertDoesNotExist()
        openApps()
        rule.onNodeWithText("All apps").assertIsDisplayed()
    }

    @Test fun holdingEmptySpaceOpensSettings() {
        openSettings()
        rule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    @Test fun tapsShortHorizontalDownwardAndCancelledSwipesKeepHome() {
        val home = rule.onNodeWithTag("home_surface")
        home.performTouchInput { click(Offset(centerX, height * 0.4f)) }
        home.assertIsDisplayed()
        home.performTouchInput { swipe(Offset(centerX, bottom - 8f), Offset(centerX, bottom - 18f), 200) }
        home.assertIsDisplayed()
        home.performTouchInput { swipe(Offset(centerX, bottom - 24f), Offset(right - 16f, bottom - 24f), 300) }
        home.assertIsDisplayed()
        home.performTouchInput { swipe(Offset(centerX, bottom - 70f), Offset(centerX, bottom - 4f), 300) }
        home.assertIsDisplayed()
        home.performTouchInput {
            down(Offset(centerX, bottom - 8f))
            moveBy(Offset(0f, -300f), 300)
            cancel()
        }
        home.assertIsDisplayed()
        rule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test fun swipeOutsideBottomDoesNotOpenDrawerAndPinDragDoesNotLaunch() {
        observeLaunch { launched ->
            val home = rule.onNodeWithTag("home_surface")
            home.performTouchInput { swipe(Offset(centerX, height * 0.5f), Offset(centerX, height * 0.3f), 300) }
            home.assertIsDisplayed()
            rule.onNodeWithTag("home_$firstId").performTouchInput { swipeUp() }
            home.assertIsDisplayed()
            assertNull(launched.get())
        }
    }

    @Test fun accessibilityActionsOpenAppsAndSettings() {
        fun action(label: String) {
            val actions = rule.onNodeWithTag("home_surface").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            rule.runOnIdle { assertTrue(actions.single { it.label == label }.action()) }
        }
        action("All apps")
        rule.onNodeWithTag("app_search").assertIsDisplayed()
        rule.onNodeWithText("Home", substring = false).performClick()
        action("Settings")
        rule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardOpensAppsAndSettingsWithoutVisibleHomeControls() {
        fun focusHome() {
            rule.onNodeWithTag("home_surface").performSemanticsAction(SemanticsActions.RequestFocus)
        }
        focusHome()
        rule.onNodeWithTag("home_surface").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("app_search").assertIsDisplayed()
        rule.onNodeWithText("Home", substring = false).performClick()
        focusHome()
        rule.onNodeWithTag("home_surface").performKeyInput { pressKey(Key.Menu) }
        rule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    private fun returnToStil() {
        rule.activity.runOnUiThread {
            rule.activity.startActivity(Intent(rule.activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("home_surface").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    }

    private fun observeLaunch(block: (AtomicReference<String?>) -> Unit) {
        val launched = AtomicReference<String?>(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { launched.set(intent.getStringExtra("activity")) }
        }
        val context = rule.activity.applicationContext
        ContextCompat.registerReceiver(context, receiver, IntentFilter("app.stil.launcher.test.FIXTURE_OPENED"), ContextCompat.RECEIVER_EXPORTED)
        try { block(launched) } finally { context.unregisterReceiver(receiver) }
    }
}
