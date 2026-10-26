package app.stil.launcher.core

import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class LauncherRulesTest {
    private val apps = (1..8).map { LaunchableApp("0:pkg$it/Main", "pkg$it", "App $it") }
    private val settings = LauncherSettings(onboarded = true, pins = apps.take(5).map { it.id })

    @Test fun `pins have a hard limit and rejected edits preserve state`() {
        val six = LauncherRules.pin(settings, apps[5]).settings
        val seventh = LauncherRules.pin(six, apps[6])
        assertEquals(6, six.pins.size)
        assertEquals(EditError.HOME_FULL, seventh.error)
        assertEquals(six, seventh.settings)
    }

    @Test fun `pinning an existing app does not duplicate it`() {
        assertEquals(settings, LauncherRules.pin(settings, apps.first()).settings)
    }

    @Test fun `hiding and unpinning the final home app are rejected`() {
        val one = settings.copy(pins = listOf(apps.first().id))
        listOf(false, true).forEach { hide ->
            assertEquals(SettingsEdit(one, EditError.LAST_HOME_APP), LauncherRules.remove(one, apps.first().id, hide))
        }
    }

    @Test fun `hiding an app removes it from home and search`() {
        val hidden = LauncherRules.remove(settings, apps.first().id, hide = true).settings
        assertFalse(apps.first().id in hidden.pins)
        assertTrue(apps.first().id in hidden.hidden)
        assertFalse(LauncherRules.search(apps, hidden, "").any { it.id == apps.first().id })
        assertEquals(apps.first(), LauncherRules.search(apps, hidden.copy(hidden = emptySet()), "App 1").single())
    }

    @Test fun `search finds original names and trimmed case insensitive aliases`() {
        val renamed = LauncherRules.rename(settings, apps.first().id, "  Go places  ").settings
        assertEquals("Go places", renamed.aliases[apps.first().id])
        assertEquals(apps.first(), LauncherRules.search(apps, renamed, "  GO PLA ").single())
        assertEquals(apps.first(), LauncherRules.search(apps, renamed, "app 1").single())
        assertTrue(LauncherRules.search(apps, renamed, "absent").isEmpty())
    }

    @Test fun `blank names preserve previous alias and long names are bounded`() {
        val original = settings.copy(aliases = mapOf(apps.first().id to "Original"))
        assertEquals(SettingsEdit(original, EditError.EMPTY_NAME), LauncherRules.rename(original, apps.first().id, "  "))
        assertEquals(32, LauncherRules.rename(original, apps.first().id, "x".repeat(100)).settings.aliases.getValue(apps.first().id).length)
    }

    @Test fun `automatic search opens only one nonblank available match including aliases`() {
        val renamed = settings.copy(aliases = mapOf(apps.first().id to "Go places"))
        assertEquals(apps.first(), LauncherRules.singleSearchMatch(apps, renamed, " GO PLA "))
        assertEquals(apps.first(), LauncherRules.singleSearchMatch(apps, renamed, "app 1"))
        assertNull(LauncherRules.singleSearchMatch(apps, settings, "app"))
        assertNull(LauncherRules.singleSearchMatch(apps, settings, "absent"))
        assertNull(LauncherRules.singleSearchMatch(apps.take(1), settings, "  "))
        assertNull(LauncherRules.singleSearchMatch(listOf(apps.first().copy(available = false)), settings, "app 1"))
    }

    @Test fun `automatic search excludes hidden and private apps and keeps profile ambiguity`() {
        val personal = apps.first()
        val work = personal.copy(id = "12:pkg1/Main", profile = ProfileKind.WORK)
        val private = personal.copy(id = "14:pkg1/Main", profile = ProfileKind.PRIVATE)
        assertEquals(personal, LauncherRules.singleSearchMatch(listOf(personal, private), settings, "app 1"))
        assertNull(LauncherRules.singleSearchMatch(listOf(private), settings, "app 1"))
        assertNull(LauncherRules.singleSearchMatch(apps, settings.copy(hidden = setOf(personal.id)), "app 1"))
        assertNull(LauncherRules.singleSearchMatch(listOf(personal, work), settings, "app 1"))
        assertNull(LauncherRules.singleSearchMatch(listOf(personal, work.copy(available = false)), settings, "app 1"))
        assertEquals(work, LauncherRules.singleSearchMatch(listOf(work), settings, "app 1"))
    }

    @Test fun `reordering preserves identity and other app order`() {
        val moved = LauncherRules.move(settings, apps[2].id, -1)
        assertEquals(listOf(apps[0].id, apps[2].id, apps[1].id, apps[3].id, apps[4].id), moved.pins)
        assertEquals(settings, LauncherRules.move(settings, apps[0].id, -1))
        assertEquals(settings, LauncherRules.move(settings, "missing", 1))
    }

    @Test fun `private apps never enter global search or home even with an alias`() {
        val private = LaunchableApp("10:secret/Main", "secret", "Bank", ProfileKind.PRIVATE)
        val value = settings.copy(aliases = mapOf(private.id to "App 1"))
        assertFalse(LauncherRules.search(apps + private, value, "").contains(private))
        assertFalse(LauncherRules.search(apps + private, value, "bank").contains(private))
        assertFalse(LauncherRules.search(apps + private, value, "App 1").contains(private))
        assertEquals(EditError.PRIVATE_HOME_APP, LauncherRules.pin(value, private).error)
    }

    @Test fun `duplicate labels across profiles retain separate identities`() {
        val personal = apps.first()
        val work = personal.copy(id = "12:pkg1/Main", profile = ProfileKind.WORK)
        val result = LauncherRules.search(listOf(personal, work), settings, "app 1")
        assertEquals(2, result.size)
        assertEquals(setOf(personal.id, work.id), result.map { it.id }.toSet())
    }

    @Test fun `uninstalled apps are removed but paused work pins survive refresh`() {
        val original = settings.copy(pins = listOf(apps.first().id, "12:work/Main", "14:private/Main", "0:removed/Main"))
        val result = LauncherRules.reconcilePins(original, listOf(apps.first(),
            LaunchableApp("14:private/Main", "private", "Private", ProfileKind.PRIVATE)), setOf(12))
        assertEquals(listOf(apps.first().id, "12:work/Main"), result.pins)
        assertTrue(LauncherRules.reconcilePins(settings, emptyList(), emptySet()).pins.isEmpty())
    }

    @Test fun `setup requires an available nonprivate app and enforces maximum`() {
        assertEquals(EditError.NO_HOME_APPS, LauncherRules.finishSetup(settings, emptyList()).error)
        assertEquals(EditError.NO_HOME_APPS, LauncherRules.finishSetup(settings, listOf(apps.first().copy(available = false))).error)
        assertEquals(EditError.NO_HOME_APPS, LauncherRules.finishSetup(settings, listOf(apps.first().copy(profile = ProfileKind.PRIVATE))).error)
        assertEquals(EditError.HOME_FULL, LauncherRules.finishSetup(settings, apps).error)
        val finished = LauncherRules.finishSetup(LauncherSettings(), listOf(apps[1], apps[1], apps[0])).settings
        assertTrue(finished.onboarded)
        assertEquals(listOf(apps[1].id, apps[0].id), finished.pins)
        assertTrue(finished.paused.isEmpty())
    }

    @Test fun `normalized persisted data cannot violate bounds`() {
        val value = settings.copy(pins = apps.map { it.id } + apps.first().id, hidden = setOf(apps[1].id), dim = 0, textSize = 2,
            aliases = mapOf("empty" to " ", "long" to "x".repeat(100))).normalized()
        assertEquals(6, value.pins.size)
        assertEquals(value.pins.distinct(), value.pins)
        assertFalse(apps[1].id in value.pins)
        assertEquals(60, value.dim)
        assertEquals(32, value.textSize)
        assertFalse(value.aliases.containsKey("empty"))
        assertEquals(32, value.aliases.getValue("long").length)
    }

    @Test fun `settings serialization preserves customizations and fills new defaults`() {
        val json = Json { ignoreUnknownKeys = true }
        val value = settings.copy(font = FontChoice.MONO, theme = ThemeMode.BLACK, wallpaper = WallpaperMode.KEEP,
            aliases = mapOf(apps.first().id to "Phone"), hidden = setOf(apps[6].id), paused = setOf(apps[2].id))
        assertEquals(value, json.decodeFromString<LauncherSettings>(json.encodeToString(value)))
        assertEquals(LauncherSettings(), json.decodeFromString<LauncherSettings>("{\"futureField\":true}"))
    }

    @Test fun `pause cannot open before three seconds and does not depend on wall clock`() {
        val pause = LaunchPause(startedAt = 100)
        assertEquals(3, pause.secondsRemaining(100))
        assertEquals(2, pause.secondsRemaining(1100))
        assertEquals(1, pause.secondsRemaining(3099))
        assertFalse(pause.canOpen(3099))
        assertTrue(pause.canOpen(3100))
        assertEquals(0, pause.secondsRemaining(9999))
    }

    @Test fun `daily rotation runs once per local date and keep prevents rotation`() {
        val today = LocalDate.of(2026, 10, 25)
        val current = settings.copy(wallpaper = WallpaperMode.DAILY, wallpaperDate = today.toString())
        assertFalse(WallpaperPolicy.shouldRotate(current, today))
        assertTrue(WallpaperPolicy.shouldRotate(current, today.plusDays(1)))
        assertTrue(WallpaperPolicy.shouldRotate(current, today, force = true))
        assertFalse(WallpaperPolicy.shouldRotate(current.copy(wallpaper = WallpaperMode.KEEP), today.plusDays(1)))
        assertTrue(WallpaperPolicy.shouldRotate(current.copy(wallpaper = WallpaperMode.KEEP), today, force = true))
        assertFalse(WallpaperPolicy.shouldRotate(current.copy(wallpaper = WallpaperMode.OFF), today, force = true))
    }
}
