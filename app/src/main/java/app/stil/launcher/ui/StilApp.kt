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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stil.launcher.LauncherViewModel
import app.stil.launcher.R
import app.stil.launcher.Screen
import app.stil.launcher.core.Alignment
import app.stil.launcher.core.FontChoice
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

@Composable
fun StilApp(model: LauncherViewModel, chooseHome: () -> Unit, openClock: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val dark = settings.theme == ThemeMode.BLACK || settings.theme == ThemeMode.SYSTEM && isSystemInDarkTheme()
    val background = if (dark) Color.Black else Color(0xFFF7F8FA)
    val foreground = if (dark) Color(0xFFF3F4F6) else Color(0xFF20242B)
    val colors = if (dark) darkColorScheme(primary = foreground, onPrimary = background, secondaryContainer = foreground,
        onSecondaryContainer = background, background = background, surface = background, onSurface = foreground)
        else lightColorScheme(primary = foreground, onPrimary = background, secondaryContainer = foreground,
            onSecondaryContainer = background, background = background, surface = background, onSurface = foreground)
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
            Scaffold(containerColor = Color.Transparent, contentColor = foreground, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                val content = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(horizontal = 28.dp)
                if (!state.ready) {
                    val loadingLabel = stringResource(R.string.loading)
                    Box(content, contentAlignment = UiAlignment.Center) { CircularProgressIndicator(Modifier.semantics { contentDescription = loadingLabel }, color = foreground) }
                } else ScreenTransition(screen) { destination -> when (destination) {
                    Screen.HOME -> HomeScreen(model, settings, content, photoHome, openClock)
                    Screen.APPS -> AppsScreen(model, settings, content)
                    Screen.SETTINGS -> SettingsScreen(model, settings, content, chooseHome)
                    Screen.FONT -> Column(content) {
                        Header(R.string.font) { model.navigate(Screen.SETTINGS) }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            FontChoice.entries.forEach { font ->
                                val selectionColor by animateColorAsState(
                                    if (settings.font == font) foreground.copy(alpha = 0.06f) else Color.Transparent,
                                    tween(120), label = "font selection")
                                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).background(selectionColor, MaterialTheme.shapes.small)
                                    .selectable(settings.font == font, role = Role.RadioButton, onClick = { model.update { it.copy(font = font) } })
                                    .testTag("font_${font.name}"), verticalAlignment = UiAlignment.CenterVertically) {
                                    Text(stringResource(fontLabel(font)), Modifier.weight(1f), fontFamily = font.family(), fontSize = 28.sp)
                                    RadioButton(selected = settings.font == font, onClick = null)
                                }
                            }
                        }
                        OutlinedButton(onClick = { model.navigate(Screen.HOME) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.preview_home)) }
                    }
                    Screen.WALLPAPER -> WallpaperScreen(model, settings, content)
                    Screen.MANAGE -> ManageScreen(model, settings, content)
                    Screen.EDIT -> EditScreen(model, settings, content)
                    Screen.HIDDEN -> HiddenScreen(model, settings, content)
                    Screen.SETUP -> SetupScreen(model, settings, content)
                    Screen.DEFAULT -> Column(content, verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.use_as_home), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.home_gestures), Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(28.dp))
                        Button(onClick = { model.navigate(Screen.HOME); chooseHome() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_stil)) }
                        TextButton(onClick = { model.navigate(Screen.HOME) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.not_now)) }
                    }
                    Screen.PAUSE -> model.pending?.let { app ->
                        PauseScreen(settings.label(app), model.remaining, content, model::confirmOpen, model::cancelPause)
                    }
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
private fun HomeScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier, photo: Boolean, openClock: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = settings.pins.mapNotNull { id -> state.catalog.apps.find { it.id == id && it.profile != ProfileKind.PRIVATE && it.id !in settings.hidden } }
    val ink = if (photo) Color.White else MaterialTheme.colorScheme.onSurface
    val now by produceState(LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(1_000) } }
    val context = LocalContext.current
    val clockFormat = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val align = if (settings.alignment == Alignment.LEFT) TextAlign.Left else TextAlign.Right
    BoxWithConstraints(modifier.homeNavigation(
        onAllApps = { model.navigate(Screen.APPS) }, onSettings = { model.navigate(Screen.SETTINGS) },
    )) {
        val scale = LocalDensity.current.fontScale
        val time = now.format(DateTimeFormatter.ofPattern(clockFormat))
        val clockSize = minOf(72f, maxWidth.value / (time.length * 0.65f * scale), maxHeight.value * 0.22f / scale).coerceAtLeast(24f)
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.clock), onClick = openClock)) {
                Text(time, Modifier.fillMaxWidth(), fontSize = clockSize.sp, fontWeight = FontWeight.Light, color = ink, textAlign = align, maxLines = 1)
                Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), Modifier.fillMaxWidth(), color = ink, textAlign = align)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = UiAlignment.BottomStart) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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
            Spacer(Modifier.fillMaxWidth().height(56.dp).homeDrawerGesture { model.navigate(Screen.APPS) })
        }
    }
}

@Composable
private fun Header(title: Int, onBack: () -> Unit) {
    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.back)) }
    Text(stringResource(title), Modifier.padding(top = 8.dp, bottom = 20.dp).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
}

@Composable
private fun AppRow(app: LaunchableApp, settings: LauncherSettings, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = UiAlignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(settings.label(app), fontSize = 22.sp)
            if (app.profile == ProfileKind.WORK) Text(stringResource(R.string.work), style = MaterialTheme.typography.labelSmall)
        }
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun AppsScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = LauncherRules.search(state.catalog.apps, settings, query)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { model.navigate(Screen.HOME) }) { Text(stringResource(R.string.home)) }
            TextButton(onClick = { model.navigate(Screen.MANAGE) }) { Text(stringResource(R.string.edit)) }
        }
        Text(stringResource(R.string.all_apps), Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.headlineLarge)
        LazyColumn(Modifier.weight(1f).testTag("app_list")) {
            if (results.isEmpty()) item {
                Text(stringResource(R.string.no_matches), Modifier.padding(vertical = 24.dp))
                if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text(stringResource(R.string.clear_search)) }
            }
            items(results.filter { it.profile == ProfileKind.PERSONAL }, key = { it.id }) { app -> AppRow(app, settings, if (app.id in settings.paused) stringResource(R.string.pause) else null) { keyboard?.hide(); model.open(app) } }
            if (results.any { it.profile == ProfileKind.WORK }) item { Text(stringResource(R.string.work_apps), Modifier.padding(top = 16.dp).semantics { heading() }) }
            items(results.filter { it.profile == ProfileKind.WORK }, key = { it.id }) { app -> AppRow(app, settings, if (app.id in settings.paused) stringResource(R.string.pause) else null) { keyboard?.hide(); model.open(app) } }
            state.catalog.profiles.filter { it.kind == ProfileKind.WORK }.forEach { profile ->
                item(key = "work_${profile.serial}") { ProfileAction(model, profile) }
            }
            if (settings.privateSpaceVisible && query.isBlank()) {
                state.catalog.profiles.filter { it.kind == ProfileKind.PRIVATE }.forEach { profile ->
                    item(key = "private_${profile.serial}") { PrivateSpace(model, settings, profile) }
                }
            }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("app_search"),
            label = { Text(stringResource(R.string.search_apps)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { results.firstOrNull { it.available }?.let { keyboard?.hide(); model.open(it) } }))
    }
}

@Composable
private fun ProfileAction(model: LauncherViewModel, profile: AppProfile) {
    TextButton(onClick = {
        if (!model.graph.catalog.setProfileQuiet(profile.serial, !profile.quiet)) model.messages.tryEmit(R.string.profile_action_failed)
    }) { Text(stringResource(if (Build.VERSION.SDK_INT < 28) R.string.manage_work else if (profile.quiet) R.string.resume_work else R.string.pause_work)) }
}

@Composable
private fun PrivateSpace(model: LauncherViewModel, settings: LauncherSettings, profile: AppProfile) {
    val state by model.state.collectAsStateWithLifecycle()
    var expanded by rememberSaveable(profile.serial) { mutableStateOf(false) }
    var query by rememberSaveable(profile.serial) { mutableStateOf("") }
    LaunchedEffect(profile.quiet) { if (profile.quiet) { expanded = false; query = "" } }
    HorizontalDivider(Modifier.padding(top = 20.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = UiAlignment.CenterVertically) {
        TextButton(onClick = { expanded = !expanded }, Modifier.weight(1f)) { Text(stringResource(R.string.private_space)) }
        TextButton(onClick = {
            expanded = false; query = ""
            if (!model.graph.catalog.setProfileQuiet(profile.serial, !profile.quiet)) model.messages.tryEmit(R.string.profile_action_failed)
        }) { Text(stringResource(if (profile.quiet) R.string.unlock else R.string.lock)) }
    }
    if (expanded && !profile.quiet) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.private_search)) }, singleLine = true)
        state.catalog.apps.filter { it.profile == ProfileKind.PRIVATE && it.id.startsWith("${profile.serial}:") &&
            it.id !in settings.hidden && (query.isBlank() || it.name.contains(query, ignoreCase = true) || settings.label(it).contains(query, ignoreCase = true))
        }.sortedBy { settings.label(it) }.forEach { app -> AppRow(app, settings) { model.open(app) } }
    }
}

@Composable
private fun <T> Choices(label: Int, selected: T, options: List<Pair<T, Int>>, onSelect: (T) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, name) ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(stringResource(name)) }, Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

@Composable
private fun SettingLink(label: Int, value: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick), verticalAlignment = UiAlignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        value?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
    }
    HorizontalDivider()
}

@Composable
private fun Toggle(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange), verticalAlignment = UiAlignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked, null)
    }
}

@Composable
private fun SettingsScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier, chooseHome: () -> Unit) {
    Column(modifier) {
        Header(R.string.settings) { model.navigate(Screen.HOME) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
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
    if (app == null) { Column(modifier) { Header(R.string.home_apps) { model.navigate(Screen.MANAGE) }; Text(stringResource(R.string.app_unavailable)) }; return }
    var label by rememberSaveable(app.id) { mutableStateOf(settings.label(app)) }
    val pinned = app.id in settings.pins
    Column(modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { model.navigate(Screen.MANAGE) }) { Text(stringResource(R.string.back)) }
        Text(app.name, Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(label, { label = it.take(LauncherSettings.MAX_NAME_LENGTH) }, Modifier.fillMaxWidth().testTag("rename_input"), label = { Text(stringResource(R.string.name)) }, singleLine = true)
        Button(onClick = { model.edit { LauncherRules.rename(it, app.id, label) } }, Modifier.fillMaxWidth(), enabled = label.isNotBlank()) { Text(stringResource(R.string.save)) }
        SettingLink(if (pinned) R.string.remove_home else R.string.add_home) { model.edit { if (pinned) LauncherRules.remove(it, app.id) else LauncherRules.pin(it, app) } }
        if (pinned) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { model.update { LauncherRules.move(it, app.id, -1) } }, Modifier.weight(1f), enabled = settings.pins.indexOf(app.id) > 0) { Text(stringResource(R.string.move_up)) }
            OutlinedButton(onClick = { model.update { LauncherRules.move(it, app.id, 1) } }, Modifier.weight(1f), enabled = settings.pins.indexOf(app.id) < settings.pins.lastIndex) { Text(stringResource(R.string.move_down)) }
        }
        Toggle(R.string.pause, app.id in settings.paused) { value -> model.update { it.copy(paused = if (value) it.paused + app.id else it.paused - app.id) } }
        Text(stringResource(R.string.pause_hint), style = MaterialTheme.typography.bodySmall)
        SettingLink(R.string.hide) { model.edit { LauncherRules.remove(it, app.id, hide = true) } }
        Text(stringResource(R.string.hide_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HiddenScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = state.catalog.apps.filter { it.id in settings.hidden && it.profile != ProfileKind.PRIVATE }
    Column(modifier) {
        Header(R.string.hidden_apps) { model.navigate(Screen.SETTINGS) }
        LazyColumn {
            if (apps.isEmpty()) item { Text(stringResource(R.string.no_hidden_apps)) }
            items(apps, key = { it.id }) { app -> AppRow(app, settings, stringResource(R.string.restore)) { model.update { it.copy(hidden = it.hidden - app.id) } } }
        }
    }
}

@Composable
private fun SetupScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps = state.catalog.apps.filter { it.profile != ProfileKind.PRIVATE && it.available }.sortedBy { it.name }
    Column(modifier) {
        Text(stringResource(R.string.app_name), Modifier.padding(top = 20.dp, bottom = 24.dp), fontSize = 40.sp)
        Text(stringResource(R.string.choose_home_apps), style = MaterialTheme.typography.headlineLarge)
        LazyColumn(Modifier.weight(1f)) {
            items(apps, key = { it.id }) { app ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(app.id in model.selection, role = Role.Checkbox, onClick = { model.toggleSelection(app.id) }), verticalAlignment = UiAlignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(settings.label(app), fontSize = 22.sp)
                        if (app.profile == ProfileKind.WORK) Text(stringResource(R.string.work), style = MaterialTheme.typography.labelSmall)
                    }
                    Checkbox(app.id in model.selection, onCheckedChange = null)
                }
            }
        }
        Text(stringResource(R.string.selected_count, model.selection.size), style = MaterialTheme.typography.bodySmall)
        Button(onClick = model::finishSetup, Modifier.fillMaxWidth().testTag("finish_setup"), enabled = model.selection.isNotEmpty()) { Text(stringResource(R.string.continue_label)) }
    }
}

@Composable
fun PauseScreen(label: String, remaining: Int, modifier: Modifier = Modifier, open: () -> Unit, cancel: () -> Unit) {
    Column(modifier) {
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.pause), style = MaterialTheme.typography.labelLarge)
        Text(label, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineLarge)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = UiAlignment.Center) {
            Text(if (remaining > 0) remaining.toString() else stringResource(R.string.ready), fontSize = 64.sp, fontWeight = FontWeight.Light)
        }
        Button(open, Modifier.fillMaxWidth().testTag("confirm_open"), enabled = remaining == 0) { Text(stringResource(R.string.open)) }
        TextButton(cancel, Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun WallpaperScreen(model: LauncherViewModel, settings: LauncherSettings, modifier: Modifier) {
    val context = LocalContext.current
    Column(modifier) {
        Header(R.string.wallpaper) { model.navigate(Screen.SETTINGS) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Surface(shape = MaterialTheme.shapes.large) {
                Box(Modifier.fillMaxWidth().height(210.dp)) {
                    if (settings.wallpaper != WallpaperMode.OFF) WallpaperImage(model, settings)
                    Text("09:41", Modifier.padding(24.dp), color = if (settings.wallpaper != WallpaperMode.OFF) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 40.sp)
                }
            }
            Choices(R.string.rotation, settings.wallpaper, listOf(WallpaperMode.OFF to R.string.off, WallpaperMode.DAILY to R.string.daily, WallpaperMode.KEEP to R.string.keep)) { value -> model.update { it.copy(wallpaper = value) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(model::nextWallpaper, Modifier.weight(1f), enabled = !model.busy && settings.wallpaper != WallpaperMode.OFF) { Text(stringResource(if (model.busy) R.string.updating else R.string.next_image)) }
                OutlinedButton(onClick = { model.navigate(Screen.HOME) }, Modifier.weight(1f)) { Text(stringResource(R.string.preview_home)) }
            }
            Text(stringResource(R.string.dim) + " " + stringResource(R.string.percent, settings.dim), Modifier.padding(top = 16.dp))
            Slider(settings.dim.toFloat(), { value -> model.update { it.copy(dim = value.toInt()) } }, valueRange = 60f..85f, steps = 4, enabled = settings.wallpaper != WallpaperMode.OFF,
                modifier = Modifier.testTag("wallpaper_dim"))
            Toggle(R.string.wifi_only, settings.wifiOnly) { value -> model.update { it.copy(wifiOnly = value) } }
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, settings.wallpaperSource.toUri())) } }) {
                Text(stringResource(R.string.photo_credit, settings.wallpaperAuthor), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.photo_provider), style = MaterialTheme.typography.bodySmall)
        }
    }
}
