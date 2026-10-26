package app.stil.launcher.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stil.launcher.LauncherViewModel
import app.stil.launcher.R
import app.stil.launcher.Screen
import app.stil.launcher.core.Alignment
import app.stil.launcher.core.FontChoice
import app.stil.launcher.core.FoldLayout
import app.stil.launcher.core.LaunchableApp
import app.stil.launcher.core.LauncherRules
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.ProfileKind
import app.stil.launcher.core.ThemeMode
import app.stil.launcher.core.WallpaperMode
import app.stil.launcher.data.AppProfile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.SYSTEM -> FontFamily.Default
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
}

private fun fontLabel(value: FontChoice): Int = when (value) {
    FontChoice.SYSTEM -> R.string.system
    FontChoice.SANS -> R.string.sans
    FontChoice.SERIF -> R.string.serif
    FontChoice.MONO -> R.string.mono
}

private fun wallpaperLabel(value: WallpaperMode): Int = when (value) {
    WallpaperMode.OFF -> R.string.off
    WallpaperMode.DAILY -> R.string.daily
    WallpaperMode.KEEP -> R.string.keep
}

// Every Material role resolves to the design tokens, so no default tint or accent leaks into controls.
private fun neutralColors(dark: Boolean, background: Color, foreground: Color): ColorScheme {
    val muted = if (dark) Color(0xFFA3A8B1) else Color(0xFF626873)
    val container = foreground.copy(alpha = 0.06f).compositeOver(background)
    val containerHigh = foreground.copy(alpha = 0.12f).compositeOver(background)
    val line = foreground.copy(alpha = 0.16f).compositeOver(background)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = foreground, onPrimary = background, primaryContainer = foreground, onPrimaryContainer = background,
        inversePrimary = background, secondary = foreground, onSecondary = background,
        secondaryContainer = containerHigh, onSecondaryContainer = foreground,
        tertiary = foreground, onTertiary = background, tertiaryContainer = containerHigh, onTertiaryContainer = foreground,
        background = background, onBackground = foreground, surface = background, onSurface = foreground,
        surfaceVariant = container, onSurfaceVariant = muted, surfaceTint = background,
        inverseSurface = foreground, inverseOnSurface = background, outline = muted, outlineVariant = line,
        surfaceBright = background, surfaceDim = container, surfaceContainerLowest = background,
        surfaceContainerLow = container, surfaceContainer = container, surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHigh,
    )
}

@Composable
fun StilApp(model: LauncherViewModel, chooseHome: () -> Unit, openClock: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val dark = settings.theme == ThemeMode.BLACK || settings.theme == ThemeMode.SYSTEM && isSystemInDarkTheme()
    val background = if (dark) Color.Black else Color(0xFFF7F8FA)
    val foreground = if (dark) Color(0xFFF3F4F6) else Color(0xFF20242B)
    val colors = neutralColors(dark, background, foreground)
    val family = settings.font.family()
    val typography = remember(family) { Typography().run { copy(
        displayLarge = displayLarge.copy(fontFamily = family), displayMedium = displayMedium.copy(fontFamily = family), displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family), headlineMedium = headlineMedium.copy(fontFamily = family), headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family), titleMedium = titleMedium.copy(fontFamily = family), titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family), bodyMedium = bodyMedium.copy(fontFamily = family), bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family), labelMedium = labelMedium.copy(fontFamily = family), labelSmall = labelSmall.copy(fontFamily = family),
    ) } }
    val screen = if (!settings.onboarded && state.ready) Screen.SETUP else model.screen
    val photoHome = screen == Screen.HOME && settings.wallpaper != WallpaperMode.OFF
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark && !photoHome
                isAppearanceLightNavigationBars = !dark && !photoHome
            }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(context.getString(it)) } }
    LaunchedEffect(state.ready, settings.onboarded) { if (state.ready && !settings.onboarded) model.prepareSetup() }
    BackHandler {
        when (screen) {
            Screen.HOME -> Unit
            Screen.PAUSE -> model.cancelPause()
            Screen.FONT, Screen.WALLPAPER, Screen.MANAGE, Screen.HIDDEN -> model.navigate(Screen.SETTINGS)
            Screen.EDIT -> model.navigate(Screen.MANAGE)
            else -> model.navigate(Screen.HOME)
        }
    }
    MaterialTheme(colorScheme = colors, typography = typography) {
        Box(Modifier.fillMaxSize().background(background)) {
            if (photoHome) WallpaperImage(model, settings)
            Scaffold(containerColor = Color.Transparent, contentColor = foreground) { padding ->
                val viewport = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
                FoldViewport(viewport) { window ->
                    if (!state.ready) {
                        val loadingLabel = stringResource(R.string.loading)
                        Box(Modifier.fillMaxSize(), contentAlignment = UiAlignment.Center) {
                            CircularProgressIndicator(Modifier.semantics { contentDescription = loadingLabel }, color = foreground)
                        }
                    } else ScreenTransition(screen) { destination ->
                        if (destination == Screen.HOME) HomeScreen(model, settings, Modifier.fillMaxSize(), window, photoHome, openClock)
                        else SafePane(window, settings.alignment) { page ->
                            when (destination) {
                                Screen.APPS -> AppsScreen(model, settings, page)
                                Screen.SETTINGS -> SettingsScreen(model, settings, page, chooseHome)
                                Screen.FONT -> FontScreen(model, settings, page)
                                Screen.WALLPAPER -> WallpaperScreen(model, settings, page)
                                Screen.MANAGE -> ManageScreen(model, settings, page)
                                Screen.EDIT -> EditScreen(model, settings, page)
                                Screen.HIDDEN -> HiddenScreen(model, settings, page)
                                Screen.SETUP -> SetupScreen(model, settings, page)
                                Screen.DEFAULT -> DefaultScreen(page, { model.navigate(Screen.HOME); chooseHome() }) { model.navigate(Screen.HOME) }
                                Screen.PAUSE -> model.pending?.let { app ->
                                    PauseScreen(settings.label(app), model.remaining, page, model::confirmOpen, model::cancelPause)
                                }
                                Screen.HOME -> Unit
                            }
                        }
                    }
                    SafePane(window, settings.alignment) { page ->
                        Box(page, contentAlignment = UiAlignment.BottomCenter) { SnackbarHost(snackbar) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperImage(model: LauncherViewModel, settings: LauncherSettings, dim: Boolean = true) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, settings.wallpaperFile) {
        value = withContext(Dispatchers.IO) { model.graph.wallpapers.currentFile(settings)?.let { BitmapFactory.decodeFile(it.path) } }
    }
    Crossfade(bitmap, Modifier.fillMaxSize(), tween(240), label = "wallpaper") { photo ->
        photo?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Image(painterResource(R.drawable.wallpaper_forest), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
    val dimAmount by animateFloatAsState(settings.dim / 100f, tween(120), label = "wallpaper dim")
    if (dim) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAmount)))
}

@Composable
private fun HomeScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier, window: FoldWindow,
    photo: Boolean, openClock: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = settings.pins.mapNotNull { id -> state.catalog.apps.find { it.id == id && it.profile != ProfileKind.PRIVATE && it.id !in settings.hidden } }
    val ink = if (photo) Color.White else MaterialTheme.colorScheme.onSurface
    val now by produceState(LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(1_000) } }
    val context = LocalContext.current
    val time = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val align = if (settings.alignment == Alignment.LEFT) TextAlign.Left else TextAlign.Right
    val placement = FoldLayout.home(window.width, window.height, window.fontScale, settings.alignment, window.folds)
    val pinsScroll = rememberScrollState()
    Box(modifier.homeNavigation(onAllApps = { model.navigate(Screen.APPS) }, onSettings = { model.navigate(Screen.SETTINGS) })) {
        HomePanes(placement, Modifier.fillMaxSize(), clock = {
            HomeClock(time, date, ink, align, openClock)
        }, apps = {
            Box(Modifier.fillMaxSize(), contentAlignment = if (settings.alignment == Alignment.LEFT) AbsoluteAlignment.BottomLeft else AbsoluteAlignment.BottomRight) {
                Column(Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(pinsScroll)) {
                    apps.forEach { app ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 54.dp)
                            .combinedClickable(onClick = { model.open(app) }, onLongClickLabel = stringResource(R.string.edit), onLongClick = { model.editApp(app) })
                            .padding(vertical = 8.dp).testTag("home_${app.id}")) {
                            Text(settings.label(app), Modifier.fillMaxWidth(), fontSize = settings.textSize.sp,
                                lineHeight = (settings.textSize * 1.3).sp, color = ink, textAlign = align)
                            if (app.profile == ProfileKind.WORK) Text(stringResource(R.string.work), Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall, color = ink, textAlign = align)
                        }
                    }
                    if (apps.isEmpty()) TextButton(onClick = { model.navigate(Screen.MANAGE) }) { Text(stringResource(R.string.choose_home_apps), color = ink) }
                }
            }
        }, openApps = { model.navigate(Screen.APPS) })
    }
}

@Composable
private fun HomeClock(time: String, date: String, ink: Color, align: TextAlign, openClock: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = LocalDensity.current.fontScale
        val compact = maxHeight.value / scale < 100
        val clockSize = minOf(72f, maxWidth.value / (time.length * 0.65f * scale),
            (if (compact) maxHeight.value * 0.8f else maxHeight.value - 48f * scale) / (1.2f * scale)).coerceAtLeast(18f)
        val click = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClickLabel = stringResource(R.string.clock), onClick = openClock)
        if (compact) Row(click, verticalAlignment = UiAlignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(time, fontSize = clockSize.sp, fontWeight = FontWeight.Light, color = ink, maxLines = 1)
            Text(date, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = ink, textAlign = align,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else Column(click.padding(top = 16.dp)) {
            Text(time, Modifier.fillMaxWidth(), fontSize = clockSize.sp, fontWeight = FontWeight.Light, color = ink, textAlign = align, maxLines = 1)
            Text(date, Modifier.fillMaxWidth(), color = ink, textAlign = align, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DefaultScreen(modifier: Modifier, chooseHome: () -> Unit, preview: () -> Unit) {
    BoxWithConstraints(modifier) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.use_as_home), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
            Muted(stringResource(R.string.home_gestures), Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(24.dp))
            Button(chooseHome, Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_stil)) }
            TextButton(preview, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.not_now)) }
        }
    }
}

@Composable
private fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Navigation text buttons sit in a 48dp bar and align their label with the screen's text edge. */
@Composable
private fun NavButton(label: Int, end: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, Modifier.offset(x = if (end) 12.dp else (-12).dp)) { Text(stringResource(label)) }
}

@Composable
private fun Header(title: String, leading: @Composable RowScope.() -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    if (LocalCompactHeight.current) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(bottom = 4.dp), verticalAlignment = UiAlignment.CenterVertically) {
            leading()
            Text(title, Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            trailing()
        }
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = UiAlignment.CenterVertically) { leading(); trailing() }
        Text(title, Modifier.padding(top = 4.dp, bottom = 16.dp).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun Header(title: Int, onBack: () -> Unit) {
    Header(stringResource(title), leading = { NavButton(R.string.back, onClick = onBack) })
}

@Composable
private fun HeaderContext(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One settings line: label, optional supporting text, optional trailing control. The modifier supplies the interaction. */
@Composable
private fun SettingRow(label: String, modifier: Modifier = Modifier, supporting: String? = null, enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = UiAlignment.CenterVertically) {
        Column(Modifier.weight(1f).alpha(if (enabled) 1f else 0.38f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supporting?.let { Muted(it, Modifier.padding(top = 2.dp)) }
        }
        trailing?.let { Spacer(Modifier.width(16.dp)); it() }
    }
}

@Composable
private fun SettingLink(label: Int, value: String? = null, supporting: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    SettingRow(stringResource(label), Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick), supporting, enabled,
        value?.let { { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } })
}

@Composable
private fun Toggle(label: Int, checked: Boolean, supporting: String? = null, onChange: (Boolean) -> Unit) {
    SettingRow(stringResource(label), Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange), supporting) {
        Switch(checked, null)
    }
}

@Composable
private fun AppRow(app: LaunchableApp, settings: LauncherSettings, trailing: String? = null, trailingAction: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = UiAlignment.CenterVertically) {
        AppLabel(app, settings, Modifier.weight(1f))
        trailing?.let {
            Spacer(Modifier.width(16.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge,
                color = if (trailingAction) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppLabel(app: LaunchableApp, settings: LauncherSettings, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(settings.label(app), style = MaterialTheme.typography.titleLarge)
        if (app.profile == ProfileKind.WORK) Text(stringResource(R.string.work), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Modifier.restoreFieldFocus(): Modifier {
    var focused by rememberSaveable { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val restore = remember { focused }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(requester) { if (restore) lifecycle.withResumed { requester.requestFocus() } }
    return focusRequester(requester).onFocusChanged {
        if (it.isFocused || lifecycle.currentState == Lifecycle.State.RESUMED) focused = it.isFocused
    }
}

@Composable
private fun AppsScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = LauncherRules.search(state.catalog.apps, settings, query.text)
    Column(modifier) {
        Header(stringResource(R.string.all_apps), leading = { NavButton(R.string.home) { model.navigate(Screen.HOME) } },
            trailing = { NavButton(R.string.edit, end = true) { model.navigate(Screen.MANAGE) } })
        LazyColumn(Modifier.weight(1f).testTag("app_list")) {
            if (results.isEmpty()) item {
                Text(stringResource(R.string.no_matches), Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (query.text.isNotEmpty()) TextButton(onClick = { query = TextFieldValue() }, Modifier.offset(x = (-12).dp)) { Text(stringResource(R.string.clear_search)) }
            }
            items(results.filter { it.profile == ProfileKind.PERSONAL }, key = { it.id }) { app -> AppRow(app, settings, if (app.id in settings.paused) stringResource(R.string.pause) else null) { keyboard?.hide(); model.open(app) } }
            if (results.any { it.profile == ProfileKind.WORK }) item { SectionLabel(stringResource(R.string.work_apps)) }
            items(results.filter { it.profile == ProfileKind.WORK }, key = { it.id }) { app -> AppRow(app, settings, if (app.id in settings.paused) stringResource(R.string.pause) else null) { keyboard?.hide(); model.open(app) } }
            state.catalog.profiles.filter { it.kind == ProfileKind.WORK }.forEach { profile ->
                item(key = "work_${profile.serial}") { ProfileAction(model, profile) }
            }
            if (settings.privateSpaceVisible && query.text.isBlank()) {
                state.catalog.profiles.filter { it.kind == ProfileKind.PRIVATE }.forEach { profile ->
                    item(key = "private_${profile.serial}") { PrivateSpace(model, settings, profile) }
                }
            }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp).testTag("app_search").restoreFieldFocus(),
            label = { Text(stringResource(R.string.search_apps)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { results.firstOrNull { it.available }?.let { keyboard?.hide(); model.open(it) } }))
    }
}

@Composable
private fun ProfileAction(model: LauncherViewModel, profile: AppProfile) {
    TextButton(onClick = {
        if (!model.graph.catalog.setProfileQuiet(profile.serial, !profile.quiet)) model.messages.tryEmit(R.string.profile_action_failed)
    }, Modifier.offset(x = (-12).dp)) { Text(stringResource(if (Build.VERSION.SDK_INT < 28) R.string.manage_work else if (profile.quiet) R.string.resume_work else R.string.pause_work)) }
}

@Composable
private fun PrivateSpace(model: LauncherViewModel, settings: LauncherSettings, profile: AppProfile) {
    val state by model.state.collectAsStateWithLifecycle()
    var expanded by rememberSaveable(profile.serial) { mutableStateOf(false) }
    var query by rememberSaveable(profile.serial, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    LaunchedEffect(profile.quiet) { if (profile.quiet) { expanded = false; query = TextFieldValue() } }
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = UiAlignment.CenterVertically) {
        TextButton(onClick = { expanded = !expanded }, Modifier.weight(1f).offset(x = (-12).dp)) {
            Text(stringResource(R.string.private_space), Modifier.weight(1f))
        }
        NavButton(if (profile.quiet) R.string.unlock else R.string.lock, end = true) {
            expanded = false; query = TextFieldValue()
            if (!model.graph.catalog.setProfileQuiet(profile.serial, !profile.quiet)) model.messages.tryEmit(R.string.profile_action_failed)
        }
    }
    if (expanded && !profile.quiet) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp).restoreFieldFocus(), label = { Text(stringResource(R.string.private_search)) }, singleLine = true)
        state.catalog.apps.filter { it.profile == ProfileKind.PRIVATE && it.id.startsWith("${profile.serial}:") &&
            it.id !in settings.hidden && (query.text.isBlank() || it.name.contains(query.text, ignoreCase = true) || settings.label(it).contains(query.text, ignoreCase = true))
        }.sortedBy { settings.label(it) }.forEach { app -> AppRow(app, settings) { model.open(app) } }
    }
}

@Composable
private fun <T> Choices(label: Int, selected: T, options: List<Pair<T, Int>>, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, name) ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(stringResource(name)) }, Modifier.heightIn(min = 40.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier, chooseHome: () -> Unit) {
    Column(modifier) {
        Header(R.string.settings) { model.navigate(Screen.HOME) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("settings_list")) {
            Choices(R.string.appearance, settings.theme, listOf(ThemeMode.LIGHT to R.string.light, ThemeMode.BLACK to R.string.dark, ThemeMode.SYSTEM to R.string.system)) { value -> model.update { it.copy(theme = value) } }
            SettingLink(R.string.font, stringResource(fontLabel(settings.font))) { model.navigate(Screen.FONT) }
            SettingLink(R.string.wallpaper, stringResource(wallpaperLabel(settings.wallpaper))) { model.navigate(Screen.WALLPAPER) }
            Choices(R.string.alignment, settings.alignment, listOf(Alignment.LEFT to R.string.left, Alignment.RIGHT to R.string.right)) { value -> model.update { it.copy(alignment = value) } }
            Choices(R.string.text_size, settings.textSize, listOf(28 to R.string.small, 32 to R.string.normal, 36 to R.string.large)) { value -> model.update { it.copy(textSize = value) } }
            SettingLink(R.string.home_apps, settings.pins.size.toString()) { model.navigate(Screen.MANAGE) }
            SettingLink(R.string.hidden_apps, settings.hidden.size.toString()) { model.navigate(Screen.HIDDEN) }
            Toggle(R.string.show_private, settings.privateSpaceVisible) { value -> model.update { it.copy(privateSpaceVisible = value) } }
            SettingLink(R.string.default_home, onClick = chooseHome)
        }
    }
}

/** Widens a selection highlight past the text edge so labels stay aligned with the rest of the screen. */
private fun Modifier.bleed(amount: Dp) = layout { measurable, constraints ->
    val extra = amount.roundToPx()
    val placeable = measurable.measure(constraints.copy(
        minWidth = constraints.minWidth + extra * 2,
        maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth))
    layout(placeable.width - extra * 2, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun FontScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier) {
        Header(R.string.font) { model.navigate(Screen.SETTINGS) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FontChoice.entries.forEach { font ->
                val selectionColor by animateColorAsState(if (settings.font == font) highlight else Color.Transparent, tween(120), label = "font selection")
                Row(Modifier.bleed(12.dp).fillMaxWidth().heightIn(min = 72.dp).clip(MaterialTheme.shapes.small).background(selectionColor)
                    .selectable(settings.font == font, role = Role.RadioButton, onClick = { model.update { it.copy(font = font) } })
                    .padding(horizontal = 12.dp).testTag("font_${font.name}"), verticalAlignment = UiAlignment.CenterVertically) {
                    Text(stringResource(fontLabel(font)), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = font.family()))
                    RadioButton(selected = settings.font == font, onClick = null)
                }
            }
        }
        OutlinedButton(onClick = { model.navigate(Screen.HOME) }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(stringResource(R.string.preview_home)) }
    }
}

@Composable
private fun ManageScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = LauncherRules.search(state.catalog.apps, settings, "")
    val missingLabel = stringResource(R.string.missing_app)
    val ordered = settings.pins.map { id -> apps.find { it.id == id } ?: LaunchableApp(id, "", missingLabel, available = false) } + apps.filterNot { it.id in settings.pins }
    Column(modifier) {
        Header(R.string.home_apps) { model.navigate(Screen.SETTINGS) }
        LazyColumn(Modifier.weight(1f)) {
            items(ordered, key = { it.id }) { app -> AppRow(app, settings, settings.pins.indexOf(app.id).takeIf { it >= 0 }?.plus(1)?.toString()) { model.editApp(app) } }
        }
    }
}

@Composable
private fun EditScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val app = state.catalog.apps.find { it.id == model.editId } ?: model.editId?.takeIf { it in settings.pins }
        ?.let { LaunchableApp(it, "", stringResource(R.string.missing_app), available = false) }
    if (app == null) {
        Column(modifier) {
            Header(R.string.home_apps) { model.navigate(Screen.MANAGE) }
            Text(stringResource(R.string.app_unavailable), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var label by rememberSaveable(app.id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.label(app))) }
    val pinned = app.id in settings.pins
    val position = settings.pins.indexOf(app.id)
    Column(modifier) {
        Header(app.name, leading = { NavButton(R.string.back) { model.navigate(Screen.MANAGE) } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(label, { value ->
                val text = value.text.take(LauncherSettings.MAX_NAME_LENGTH)
                label = value.copy(text = text, selection = TextRange(value.selection.start.coerceAtMost(text.length), value.selection.end.coerceAtMost(text.length)))
            }, Modifier.fillMaxWidth().testTag("rename_input").restoreFieldFocus(), label = { Text(stringResource(R.string.name)) }, singleLine = true)
            Button(onClick = { model.edit { LauncherRules.rename(it, app.id, label.text) } }, Modifier.fillMaxWidth().padding(top = 12.dp), enabled = label.text.isNotBlank()) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(16.dp))
            SettingLink(if (pinned) R.string.remove_home else R.string.add_home) { model.edit { if (pinned) LauncherRules.remove(it, app.id) else LauncherRules.pin(it, app) } }
            if (pinned) {
                SettingLink(R.string.move_up, enabled = position > 0) { model.update { LauncherRules.move(it, app.id, -1) } }
                SettingLink(R.string.move_down, enabled = position < settings.pins.lastIndex) { model.update { LauncherRules.move(it, app.id, 1) } }
            }
            Toggle(R.string.pause, app.id in settings.paused, stringResource(R.string.pause_hint)) { value -> model.update { it.copy(paused = if (value) it.paused + app.id else it.paused - app.id) } }
            SettingLink(R.string.hide, supporting = stringResource(R.string.hide_hint)) { model.edit { LauncherRules.remove(it, app.id, hide = true) } }
        }
    }
}

@Composable
private fun HiddenScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = state.catalog.apps.filter { it.id in settings.hidden && it.profile != ProfileKind.PRIVATE }
    Column(modifier) {
        Header(R.string.hidden_apps) { model.navigate(Screen.SETTINGS) }
        LazyColumn {
            if (apps.isEmpty()) item {
                Text(stringResource(R.string.no_hidden_apps), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(apps, key = { it.id }) { app -> AppRow(app, settings, stringResource(R.string.restore), trailingAction = true) { model.update { it.copy(hidden = it.hidden - app.id) } } }
        }
    }
}

@Composable
private fun SetupScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = state.catalog.apps.filter { it.profile != ProfileKind.PRIVATE && it.available }.sortedBy { it.name }
    Column(modifier) {
        Header(stringResource(R.string.choose_home_apps), leading = { HeaderContext(stringResource(R.string.app_name)) })
        LazyColumn(Modifier.weight(1f)) {
            items(apps, key = { it.id }) { app ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(app.id in model.selection, role = Role.Checkbox, onClick = { model.toggleSelection(app.id) })
                    .padding(vertical = 8.dp), verticalAlignment = UiAlignment.CenterVertically) {
                    AppLabel(app, settings, Modifier.weight(1f))
                    Checkbox(app.id in model.selection, onCheckedChange = null)
                }
            }
        }
        if (LocalCompactHeight.current) Row(Modifier.fillMaxWidth(), verticalAlignment = UiAlignment.CenterVertically) {
            Muted(stringResource(R.string.selected_count, model.selection.size), Modifier.weight(1f).padding(end = 12.dp))
            Button(onClick = model::finishSetup, Modifier.testTag("finish_setup"), enabled = model.selection.isNotEmpty()) { Text(stringResource(R.string.continue_label)) }
        } else {
            Muted(stringResource(R.string.selected_count, model.selection.size), Modifier.padding(vertical = 12.dp))
            Button(onClick = model::finishSetup, Modifier.fillMaxWidth().testTag("finish_setup"), enabled = model.selection.isNotEmpty()) { Text(stringResource(R.string.continue_label)) }
        }
    }
}

@Composable
fun PauseScreen(label: String, remaining: Int, modifier: Modifier = Modifier, open: () -> Unit, cancel: () -> Unit) {
    Column(modifier) {
        Header(label, leading = { HeaderContext(stringResource(R.string.pause)) })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = UiAlignment.Center) {
            BoxWithConstraints {
                val scale = LocalDensity.current.fontScale
                val label = if (remaining > 0) remaining.toString() else stringResource(R.string.ready)
                val size = minOf(57f, maxHeight.value / (1.3f * scale), maxWidth.value / (label.length * 0.65f * scale)).coerceAtLeast(12f)
                Text(label, fontSize = size.sp, fontWeight = FontWeight.Light)
            }
        }
        Button(open, Modifier.fillMaxWidth().testTag("confirm_open"), enabled = remaining == 0) { Text(stringResource(R.string.open)) }
        TextButton(cancel, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun WallpaperScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val context = LocalContext.current
    val photo = settings.wallpaper != WallpaperMode.OFF
    Column(modifier) {
        Header(R.string.wallpaper) { model.navigate(Screen.SETTINGS) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.fillMaxWidth().height(168.dp)) {
                    if (photo) WallpaperImage(model, settings)
                    Text("09:41", Modifier.padding(20.dp), color = if (photo) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light)
                }
            }
            Spacer(Modifier.height(8.dp))
            Choices(R.string.rotation, settings.wallpaper, listOf(WallpaperMode.OFF to R.string.off, WallpaperMode.DAILY to R.string.daily, WallpaperMode.KEEP to R.string.keep)) { value -> model.update { it.copy(wallpaper = value) } }
            SettingLink(if (model.busy) R.string.updating else R.string.next_image, enabled = !model.busy && photo, onClick = model::nextWallpaper)
            SettingRow(stringResource(R.string.dim), enabled = photo) {
                Text(stringResource(R.string.percent, settings.dim), Modifier.alpha(if (photo) 1f else 0.38f), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(settings.dim.toFloat(), { value -> model.update { it.copy(dim = value.toInt()) } }, valueRange = 60f..85f, steps = 4, enabled = photo,
                modifier = Modifier.testTag("wallpaper_dim"))
            Toggle(R.string.wifi_only, settings.wifiOnly) { value -> model.update { it.copy(wifiOnly = value) } }
            Muted(stringResource(R.string.photo_credit, settings.wallpaperAuthor), Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(role = Role.Button) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, settings.wallpaperSource.toUri())) } }
                .wrapContentHeight(UiAlignment.CenterVertically))
            Muted(stringResource(R.string.photo_provider))
        }
        OutlinedButton(onClick = { model.navigate(Screen.HOME) }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(stringResource(R.string.preview_home)) }
    }
}
