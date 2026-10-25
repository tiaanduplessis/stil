package app.stil.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.stil.launcher.StilApplication
import app.stil.launcher.core.LauncherSettings
import app.stil.launcher.core.WallpaperMode
import app.stil.launcher.core.WallpaperPolicy
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class WallpaperUpdate { CHANGED, UNCHANGED, WAITING_FOR_WIFI, RETRY, NOT_ALLOWED }

class WallpaperRepository(
    private val context: Context,
    private val settings: SettingsStore,
    private val client: PhotoSource = PhotoClient(),
) {
    private val mutex = Mutex()
    private val directory = File(context.filesDir, "wallpapers").apply { mkdirs() }

    fun currentFile(value: LauncherSettings): File? = value.wallpaperFile
        .takeIf { it.matches(Regex("[a-zA-Z0-9-]+\\.jpg")) }
        ?.let { File(directory, it) }?.takeIf(File::isFile)

    suspend fun rotate(force: Boolean = false): WallpaperUpdate = withContext(Dispatchers.IO) {
        mutex.withLock {
            val before = settings.current()
            if (!WallpaperPolicy.shouldRotate(before, LocalDate.now(), force)) return@withLock WallpaperUpdate.UNCHANGED
            if (before.wifiOnly && !isUnmetered()) return@withLock WallpaperUpdate.WAITING_FOR_WIFI
            val manager = context.getSystemService(WallpaperManager::class.java)
            if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return@withLock WallpaperUpdate.NOT_ALLOWED
            var candidate: File? = null
            try {
                val downloaded = client.download(before.wallpaperId)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(downloaded.bytes, 0, downloaded.bytes.size, bounds)
                if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) throw IOException("Invalid photo dimensions")
                val bitmap = BitmapFactory.decodeByteArray(downloaded.bytes, 0, downloaded.bytes.size)
                    ?: throw IOException("Invalid photo data")
                try {
                    candidate = File(directory, "${UUID.randomUUID()}.jpg").also { it.writeBytes(downloaded.bytes) }
                    val latest = settings.current()
                    if (latest.wallpaper != before.wallpaper || latest.wallpaper == WallpaperMode.OFF) {
                        candidate.delete()
                        return@withLock WallpaperUpdate.UNCHANGED
                    }
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    val fileName = candidate.name
                    settings.update {
                        it.copy(
                            wallpaperId = downloaded.photo.id,
                            wallpaperFile = fileName,
                            wallpaperDate = LocalDate.now().toString(),
                            wallpaperAuthor = downloaded.photo.author,
                            wallpaperSource = downloaded.photo.url,
                        )
                    }
                    directory.listFiles()?.filter { it.name != fileName && it.name != before.wallpaperFile }?.forEach(File::delete)
                    WallpaperUpdate.CHANGED
                } finally {
                    bitmap.recycle()
                }
            } catch (cancelled: CancellationException) {
                candidate?.delete()
                throw cancelled
            } catch (_: IOException) {
                candidate?.delete()
                WallpaperUpdate.RETRY
            } catch (_: SecurityException) {
                candidate?.delete()
                WallpaperUpdate.NOT_ALLOWED
            }
        }
    }

    private fun isUnmetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
    }

    fun schedule(value: LauncherSettings) {
        val manager = WorkManager.getInstance(context)
        if (value.wallpaper != WallpaperMode.DAILY) {
            manager.cancelUniqueWork(DAILY_WORK)
            manager.cancelUniqueWork(REFRESH_WORK)
            return
        }
        val constraints = Constraints.Builder().setRequiredNetworkType(if (value.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
        manager.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS, 1, TimeUnit.HOURS).setConstraints(constraints).build())
        manager.enqueueUniqueWork(REFRESH_WORK, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DailyWallpaperWorker>().setConstraints(constraints).build())
    }

    companion object {
        const val DAILY_WORK = "daily-wallpaper"
        const val REFRESH_WORK = "refresh-wallpaper"
    }
}

class DailyWallpaperWorker(context: Context, parameters: WorkerParameters, private val repository: WallpaperRepository) : CoroutineWorker(context, parameters) {
    constructor(context: Context, parameters: WorkerParameters) : this(context, parameters, (context.applicationContext as StilApplication).graph.wallpapers)

    override suspend fun doWork(): Result = when (repository.rotate()) {
        WallpaperUpdate.RETRY, WallpaperUpdate.WAITING_FOR_WIFI -> Result.retry()
        WallpaperUpdate.NOT_ALLOWED -> Result.failure()
        WallpaperUpdate.CHANGED, WallpaperUpdate.UNCHANGED -> Result.success()
    }
}
