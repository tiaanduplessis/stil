package app.stil.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import app.stil.launcher.core.FontChoice
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.WallpaperMode
import app.stil.launcher.data.DownloadedPhoto
import app.stil.launcher.data.Photo
import app.stil.launcher.data.PhotoSource
import app.stil.launcher.data.SettingsStore
import app.stil.launcher.data.LaunchStatsStore
import app.stil.launcher.core.LaunchableApp
import app.stil.launcher.data.WallpaperRepository
import app.stil.launcher.data.WallpaperUpdate
import app.stil.launcher.data.DailyWallpaperWorker
import java.io.File
import java.io.IOException
import java.util.UUID
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageAndWallpaperTest {
    private val context = ApplicationProvider.getApplicationContext<StilApplication>()
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var store: SettingsStore
    private lateinit var data: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

    @Before fun prepare() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        file = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        data = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        store = SettingsStore(data)
    }
    @After fun clean() { scope.cancel(); file.delete() }

    @Test fun settingsPersistThroughASecondStoreReader() = runBlocking {
        val expected = LauncherSettings(onboarded = true, pins = listOf("0:pkg/Main"), font = FontChoice.SERIF,
            hidden = setOf("0:hidden/Main"), aliases = mapOf("0:pkg/Main" to "Phone"), wallpaper = WallpaperMode.KEEP)
        store.update { expected }
        assertEquals(expected, SettingsStore(data).current())
        assertTrue(file.length() > 0)
    }

    @Test fun corruptSettingsRecoverToUsableDefaults() = runBlocking {
        data.edit { it[stringPreferencesKey("settings_v1")] = "not-json" }
        assertEquals(LauncherSettings(), store.current())
        store.update { it.copy(font = FontChoice.MONO) }
        assertEquals(FontChoice.MONO, store.current().font)
    }

    @Test fun concurrentLaunchesPersistWithoutLosingSettings() = runBlocking {
        store.update { it.copy(font = FontChoice.SERIF) }
        val stats = LaunchStatsStore(data)
        val app = LaunchableApp("0:pkg/Main", "pkg", "Maps")
        coroutineScope { repeat(20) { launch { stats.record(app) } } }
        assertEquals(20, LaunchStatsStore(data).history.first().days[LocalDate.now().toString()]?.get(app.id))
        assertEquals(FontChoice.SERIF, SettingsStore(data).current().font)
        assertTrue(file.length() > 0)
    }

    @Test fun legacySettingsAndCorruptStatsRecoverIndependently() = runBlocking {
        store.update { it.copy(font = FontChoice.MONO) }
        val stats = LaunchStatsStore(data)
        assertTrue(stats.history.first().days.isEmpty())
        data.edit { it[stringPreferencesKey("launch_stats_v1")] = "not-json" }
        assertTrue(stats.history.first().days.isEmpty())
        val app = LaunchableApp("0:pkg/Main", "pkg", "Maps")
        stats.record(app)
        assertEquals(1, stats.history.first().days[LocalDate.now().toString()]?.get(app.id))
        assertEquals(FontChoice.MONO, store.current().font)
    }

    @Test fun providerFailureRetainsPreviousImageAndMetadata() = runBlocking {
        val before = LauncherSettings(wallpaper = WallpaperMode.DAILY, wallpaperId = "old", wallpaperDate = "2000-01-01", wifiOnly = false)
        store.update { before }
        val source = PhotoSource { throw IOException("Provider unavailable") }
        assertEquals(WallpaperUpdate.RETRY, WallpaperRepository(context, store, source).rotate())
        assertEquals(before, store.current())
    }

    @Test fun invalidImageCannotReplaceTheCachedPhoto() = runBlocking {
        val previous = File(context.filesDir, "wallpapers/test-${UUID.randomUUID()}.jpg").apply { parentFile!!.mkdirs(); writeText("previous-photo") }
        try {
            val before = LauncherSettings(wallpaper = WallpaperMode.DAILY, wallpaperFile = previous.name, wallpaperId = "old", wallpaperDate = "2000-01-01", wifiOnly = false)
            store.update { before }
            val source = PhotoSource { DownloadedPhoto(Photo("42", "Author", "https://unsplash.com/photos/test", 1080, 2160), "invalid photo".toByteArray()) }
            assertEquals(WallpaperUpdate.RETRY, WallpaperRepository(context, store, source).rotate())
            assertEquals(before, store.current())
            assertEquals("previous-photo", previous.readText())
        } finally { previous.delete() }
    }

    @Test fun offAndKeepModesAvoidBackgroundNetworkCalls() = runBlocking {
        var calls = 0
        val source = PhotoSource { calls++; throw IOException("Unexpected download") }
        val repository = WallpaperRepository(context, store, source)
        assertEquals(WallpaperUpdate.UNCHANGED, repository.rotate(force = true))
        store.update { it.copy(wallpaper = WallpaperMode.KEEP) }
        assertEquals(WallpaperUpdate.UNCHANGED, repository.rotate())
        assertEquals(0, calls)
    }

    @Test fun cachedImagePathCannotEscapeTheWallpaperDirectory() = runBlocking {
        val repository = WallpaperRepository(context, store)
        assertNull(repository.currentFile(LauncherSettings(wallpaperFile = "../../secret.jpg")))
        assertNull(repository.currentFile(LauncherSettings(wallpaperFile = "/data/secret.jpg")))
    }

    @Test fun enabledWorkerAppliesAndCachesOneImagePerDate() = runBlocking {
        val bitmap = android.graphics.Bitmap.createBitmap(32, 64, android.graphics.Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
        var calls = 0
        val source = PhotoSource { calls++; DownloadedPhoto(Photo("42", "Test photographer", "https://unsplash.com/photos/test", 1080, 2160), bytes) }
        val repository = WallpaperRepository(context, store, source)
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: android.content.Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                DailyWallpaperWorker(appContext, workerParameters, repository)
        }
        store.update { it.copy(wallpaper = WallpaperMode.DAILY, wifiOnly = false) }
        try {
            val worker = TestListenableWorkerBuilder<DailyWallpaperWorker>(context).setWorkerFactory(factory).build()
            assertEquals(ListenableWorker.Result.success(), worker.doWork())
            val saved = store.current()
            assertEquals("42", saved.wallpaperId)
            assertEquals(LocalDate.now().toString(), saved.wallpaperDate)
            assertNotNull(repository.currentFile(saved))
            assertEquals("Test photographer", saved.wallpaperAuthor)
            assertEquals(ListenableWorker.Result.success(), TestListenableWorkerBuilder<DailyWallpaperWorker>(context).setWorkerFactory(factory).build().doWork())
            assertEquals(1, calls)
        } finally { repository.currentFile(store.current())?.delete() }
    }
}
