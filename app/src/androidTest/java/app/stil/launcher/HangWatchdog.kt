package app.stil.launcher

import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Timer
import java.util.TimerTask

/** Logs every thread's stack if a test runs past [limitMs]. It never interrupts the test, so it only explains a stall. */
class HangWatchdog(private val limitMs: Long = 90_000) : TestWatcher() {
    private var timer: Timer? = null

    override fun starting(description: Description) {
        timer = Timer("hang-watchdog", true).apply {
            schedule(object : TimerTask() {
                override fun run() {
                    Log.e(TAG, "${description.displayName} has run for ${limitMs / 1000}s")
                    Thread.getAllStackTraces().forEach { (thread, stack) ->
                        Log.e(TAG, "${thread.name} (${thread.state})\n" + stack.joinToString("\n") { "    at $it" })
                    }
                }
            }, limitMs)
        }
    }

    override fun finished(description: Description) {
        timer?.cancel()
    }

    private companion object {
        const val TAG = "StilHang"
    }
}
