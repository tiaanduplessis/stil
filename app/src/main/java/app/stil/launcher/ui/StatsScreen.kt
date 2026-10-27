package app.stil.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stil.launcher.LauncherViewModel
import app.stil.launcher.R
import app.stil.launcher.Screen
import app.stil.launcher.core.DailyLaunchCount
import app.stil.launcher.core.LaunchableApp
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.ProfileKind
import app.stil.launcher.core.summarizeLaunches
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

@Composable
internal fun StatsScreen(model: LauncherViewModel, settings: LauncherSettings, apps: List<LaunchableApp>, modifier: Modifier) {
    val state by model.stats.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableIntStateOf(7) }
    val today by produceState(LocalDate.now()) {
        while (true) { delay(60_000); value = LocalDate.now() }
    }
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val numbers = NumberFormat.getIntegerInstance()
    Column(modifier) {
        Header(R.string.launch_stats) { model.navigate(Screen.SETTINGS) }
        LazyColumn(Modifier.weight(1f).testTag("stats_list"), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to R.string.today, 7 to R.string.last_7_days, 30 to R.string.last_30_days).forEach { (days, label) ->
                        FilterChip(period == days, { period = days }, label = { Text(stringResource(label)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                    }
                }
                Text(if (period == 1) today.format(dates) else stringResource(R.string.stats_date_range,
                    today.minusDays(period - 1L).format(dates), today.format(dates)),
                    Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val history = state.history
            when {
                state.failed -> item { Text(stringResource(R.string.stats_load_failed)) }
                history == null -> item { CircularProgressIndicator() }
                else -> {
                    val summary = summarizeLaunches(history, apps, settings, period, today)
                    item {
                        Column(Modifier.semantics(mergeDescendants = true) {}) {
                            Text(numbers.format(summary.total), Modifier.testTag("stats_total"), style = MaterialTheme.typography.displayLarge)
                            Text(pluralStringResource(R.plurals.stats_opens, summary.total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (period > 1) item {
                        StatsHeading(R.string.daily_opens)
                        DailyChart(summary.days, dates, numbers)
                    }
                    if (summary.apps.isEmpty()) item {
                        Text(stringResource(R.string.stats_empty), Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        item { StatsHeading(R.string.most_opened) }
                        items(summary.apps, key = { it.app.id }) { entry ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {}) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(settings.label(entry.app), style = MaterialTheme.typography.titleLarge)
                                        if (entry.app.profile == ProfileKind.WORK) Text(stringResource(R.string.work),
                                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(numbers.format(entry.count), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleLarge)
                                }
                                LinearProgressIndicator(progress = { entry.count.toFloat() / summary.apps.first().count },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clearAndSetSemantics {},
                                    color = MaterialTheme.colorScheme.onSurface, trackColor = MaterialTheme.colorScheme.outlineVariant,
                                    gapSize = 0.dp, drawStopIndicator = {})
                            }
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 16.dp))
                        Text(stringResource(R.string.stats_scope), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeading(label: Int) {
    Text(stringResource(label), Modifier.padding(top = 24.dp, bottom = 16.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DailyChart(days: List<DailyLaunchCount>, dates: DateTimeFormatter, numbers: NumberFormat) {
    val peak = days.maxOf { it.count }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(108.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val description = stringResource(R.string.stats_day_count, day.date.format(dates), numbers.format(day.count))
            Box(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description }, contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().height(if (day.count == 0L) 2.dp else (108 * day.count.toFloat() / peak).dp.coerceAtLeast(3.dp))
                    .background(if (day.count == 0L) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(days.first().date.format(dates), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(days.last().date.format(dates), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
    }
}
