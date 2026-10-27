package app.stil.launcher.core

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class LaunchHistory(val days: Map<String, Map<String, Int>> = emptyMap()) {
    fun retained(today: LocalDate): LaunchHistory = copy(days = days.filterKeys { value ->
        val date = runCatching { LocalDate.parse(value) }.getOrNull()
        date != null && !date.isBefore(today.minusDays(RETENTION_DAYS - 1L)) && !date.isAfter(today)
    }.mapValues { (_, counts) -> counts.filterValues { it > 0 } })

    fun record(app: LaunchableApp, today: LocalDate): LaunchHistory {
        val history = retained(today)
        if (app.profile == ProfileKind.PRIVATE || !app.available) return history
        val date = today.toString()
        val counts = history.days[date].orEmpty()
        val count = ((counts[app.id] ?: 0).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return history.copy(days = history.days + (date to (counts + (app.id to count))))
    }

    companion object { const val RETENTION_DAYS = 30 }
}

data class AppLaunchCount(val app: LaunchableApp, val count: Long)
data class DailyLaunchCount(val date: LocalDate, val count: Long)
data class LaunchSummary(val apps: List<AppLaunchCount>, val days: List<DailyLaunchCount>) {
    val total: Long get() = apps.sumOf { it.count }
}

fun summarizeLaunches(history: LaunchHistory, apps: List<LaunchableApp>, settings: LauncherSettings,
    periodDays: Int, today: LocalDate): LaunchSummary {
    val visible = apps.filter { it.profile != ProfileKind.PRIVATE && it.id !in settings.hidden }.distinctBy { it.id }
    val ids = visible.map { it.id }.toSet()
    val days = (periodDays.coerceIn(1, LaunchHistory.RETENTION_DAYS) - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DailyLaunchCount(date, history.days[date.toString()].orEmpty().filterKeys { it in ids }.values.sumOf { it.coerceAtLeast(0).toLong() })
    }
    val totals = visible.map { app -> AppLaunchCount(app, days.sumOf { day ->
        history.days[day.date.toString()]?.get(app.id)?.coerceAtLeast(0)?.toLong() ?: 0L
    }) }.filter { it.count > 0 }.sortedWith(compareByDescending<AppLaunchCount> { it.count }.thenBy { settings.label(it.app) }.thenBy { it.app.id })
    return LaunchSummary(totals, days)
}
