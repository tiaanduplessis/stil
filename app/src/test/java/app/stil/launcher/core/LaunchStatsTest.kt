package app.stil.launcher.core

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class LaunchStatsTest {
    private val today = LocalDate.of(2026, 10, 27)
    private val personal = LaunchableApp("0:pkg/Main", "pkg", "Maps")
    private val work = personal.copy(id = "10:pkg/Main", profile = ProfileKind.WORK)
    private val privateApp = personal.copy(id = "11:pkg/Main", profile = ProfileKind.PRIVATE)

    @Test fun `daily counts retain profile identities and exclude private and unavailable apps`() {
        val history = LaunchHistory().record(personal, today).record(personal, today).record(work, today)
            .record(privateApp, today).record(personal.copy(available = false), today)
        assertEquals(mapOf(personal.id to 2, work.id to 1), history.days[today.toString()])
    }

    @Test fun `periods include today and fill missing days with zero`() {
        val history = LaunchHistory(mapOf(
            today.toString() to mapOf(personal.id to 2),
            today.minusDays(6).toString() to mapOf(personal.id to 3),
            today.minusDays(7).toString() to mapOf(work.id to 4),
            today.minusDays(29).toString() to mapOf(work.id to 5),
            today.minusDays(30).toString() to mapOf(personal.id to 99),
            today.plusDays(1).toString() to mapOf(personal.id to 99),
        ))
        val apps = listOf(personal, work)
        assertEquals(2L, summarizeLaunches(history, apps, LauncherSettings(), 1, today).total)
        val week = summarizeLaunches(history, apps, LauncherSettings(), 7, today)
        assertEquals(5L, week.total)
        assertEquals(listOf(3L, 0L, 0L, 0L, 0L, 0L, 2L), week.days.map { it.count })
        assertEquals(listOf(work.id, personal.id), summarizeLaunches(history, apps, LauncherSettings(), 30, today).apps.map { it.app.id })
        assertEquals(14L, summarizeLaunches(history, apps, LauncherSettings(), 30, today).total)
    }

    @Test fun `hidden private and removed apps never leak into totals or charts`() {
        val history = LaunchHistory(mapOf(today.toString() to mapOf(personal.id to 2, work.id to 4, privateApp.id to 8, "removed" to 16)))
        val summary = summarizeLaunches(history, listOf(personal, work, privateApp), LauncherSettings(hidden = setOf(work.id)), 7, today)
        assertEquals(2L, summary.total)
        assertEquals(2L, summary.days.sumOf { it.count })
        assertEquals(listOf(personal.id), summary.apps.map { it.app.id })
    }

    @Test fun `recording prunes old future invalid and negative buckets without changing recent counts`() {
        val old = LaunchHistory(mapOf(
            "bad-date" to mapOf(personal.id to 10),
            today.minusDays(30).toString() to mapOf(personal.id to 10),
            today.minusDays(29).toString() to mapOf(work.id to 3),
            today.plusDays(1).toString() to mapOf(personal.id to 10),
            today.toString() to mapOf(personal.id to -1),
        ))
        val recorded = old.record(personal, today)
        assertEquals(setOf(today.toString(), today.minusDays(29).toString()), recorded.days.keys)
        assertEquals(1, recorded.days[today.toString()]?.get(personal.id))
        assertEquals(3, recorded.days[today.minusDays(29).toString()]?.get(work.id))
    }
}
