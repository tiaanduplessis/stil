package app.stil.launcher.core

import java.text.Collator
import java.time.LocalDate
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable enum class ThemeMode { SYSTEM, LIGHT, BLACK }
@Serializable enum class FontChoice { SYSTEM, SANS, SERIF, MONO }
@Serializable enum class Alignment { LEFT, RIGHT }
@Serializable enum class WallpaperMode { OFF, DAILY, KEEP }
enum class ProfileKind { PERSONAL, WORK, PRIVATE }

data class LaunchableApp(
    val id: String,
    val packageName: String,
    val name: String,
    val profile: ProfileKind = ProfileKind.PERSONAL,
    val available: Boolean = true,
)

@Serializable
data class LauncherSettings(
    val onboarded: Boolean = false,
    val pins: List<String> = emptyList(),
    val aliases: Map<String, String> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val paused: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val font: FontChoice = FontChoice.SYSTEM,
    val alignment: Alignment = Alignment.LEFT,
    val textSize: Int = 32,
    val wallpaper: WallpaperMode = WallpaperMode.OFF,
    val dim: Int = 65,
    val wifiOnly: Boolean = true,
    val wallpaperDate: String = "",
    val wallpaperId: String = "",
    val wallpaperFile: String = "",
    val wallpaperAuthor: String = "Alvin Engler",
    val wallpaperSource: String = "https://unsplash.com/photos/aerial-view-of-trees-LnbWqObX0TQ",
    val privateSpaceVisible: Boolean = true,
) {
    fun label(app: LaunchableApp): String = aliases[app.id]?.takeIf(String::isNotBlank) ?: app.name

    fun normalized(): LauncherSettings = copy(
        pins = pins.distinct().filterNot(hidden::contains).take(MAX_PINS),
        aliases = aliases.mapValues { it.value.trim().take(MAX_NAME_LENGTH) }.filterValues(String::isNotEmpty),
        textSize = textSize.takeIf { it in setOf(28, 32, 36) } ?: 32,
        dim = dim.coerceIn(60, 85),
    )

    companion object {
        const val MAX_PINS = 6
        const val MAX_NAME_LENGTH = 32
    }
}

enum class EditError { HOME_FULL, LAST_HOME_APP, EMPTY_NAME, PRIVATE_HOME_APP, NO_HOME_APPS }
data class SettingsEdit(val settings: LauncherSettings, val error: EditError? = null)

object LauncherRules {
    fun reconcilePins(settings: LauncherSettings, apps: List<LaunchableApp>, pausedWorkProfiles: Set<Long>): LauncherSettings =
        settings.copy(pins = settings.pins.filter { id ->
            apps.any { it.id == id && it.profile != ProfileKind.PRIVATE } || pausedWorkProfiles.any { id.startsWith("$it:") }
        })

    fun search(apps: List<LaunchableApp>, settings: LauncherSettings, query: String): List<LaunchableApp> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val collator = Collator.getInstance()
        return apps.filter {
            it.profile != ProfileKind.PRIVATE && it.id !in settings.hidden &&
                (needle.isEmpty() || it.name.lowercase(Locale.ROOT).contains(needle) ||
                    settings.label(it).lowercase(Locale.ROOT).contains(needle))
        }.sortedWith { a, b ->
            val labelOrder = collator.compare(settings.label(a), settings.label(b))
            if (labelOrder != 0) labelOrder else a.id.compareTo(b.id)
        }
    }

    fun pin(settings: LauncherSettings, app: LaunchableApp): SettingsEdit = when {
        app.profile == ProfileKind.PRIVATE -> SettingsEdit(settings, EditError.PRIVATE_HOME_APP)
        app.id in settings.pins -> SettingsEdit(settings)
        settings.pins.size >= LauncherSettings.MAX_PINS -> SettingsEdit(settings, EditError.HOME_FULL)
        else -> SettingsEdit(settings.copy(pins = settings.pins + app.id, hidden = settings.hidden - app.id))
    }

    fun remove(settings: LauncherSettings, id: String, hide: Boolean = false): SettingsEdit {
        if (id in settings.pins && settings.pins.size == 1) return SettingsEdit(settings, EditError.LAST_HOME_APP)
        return SettingsEdit(settings.copy(pins = settings.pins - id, hidden = if (hide) settings.hidden + id else settings.hidden))
    }

    fun rename(settings: LauncherSettings, id: String, name: String): SettingsEdit {
        val label = name.trim().take(LauncherSettings.MAX_NAME_LENGTH)
        return if (label.isBlank()) SettingsEdit(settings, EditError.EMPTY_NAME)
        else SettingsEdit(settings.copy(aliases = settings.aliases + (id to label)))
    }

    fun move(settings: LauncherSettings, id: String, delta: Int): LauncherSettings {
        val pins = settings.pins.toMutableList()
        val from = pins.indexOf(id)
        val to = from + delta
        if (from !in pins.indices || to !in pins.indices) return settings
        pins.add(to, pins.removeAt(from))
        return settings.copy(pins = pins)
    }

    fun finishSetup(settings: LauncherSettings, selected: List<LaunchableApp>): SettingsEdit {
        val pins = selected.filter { it.profile != ProfileKind.PRIVATE && it.available }.map { it.id }.distinct()
        return when {
            pins.isEmpty() -> SettingsEdit(settings, EditError.NO_HOME_APPS)
            pins.size > LauncherSettings.MAX_PINS -> SettingsEdit(settings, EditError.HOME_FULL)
            else -> SettingsEdit(settings.copy(onboarded = true, pins = pins, hidden = settings.hidden - pins.toSet()))
        }
    }
}

object WallpaperPolicy {
    fun shouldRotate(settings: LauncherSettings, today: LocalDate, force: Boolean = false): Boolean =
        settings.wallpaper != WallpaperMode.OFF &&
            (force || settings.wallpaper == WallpaperMode.DAILY && settings.wallpaperDate != today.toString())
}

class LaunchPause(private val startedAt: Long, private val durationMillis: Long = 3_000) {
    fun secondsRemaining(now: Long): Int = ((durationMillis - (now - startedAt)).coerceAtLeast(0) + 999).div(1_000).toInt()
    fun canOpen(now: Long): Boolean = now - startedAt >= durationMillis
}
