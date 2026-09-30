package com.youki.dex.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * ClauDEX event journal: one line per mode/orientation/focus/launch event,
 * appended to <external files>/claudex-eventos.log and mirrored to logcat
 * under the "ClauDEX" tag.
 *
 * Why a file: logcat is a ring buffer - on a SM-A055M it held only a few
 * hours, so a glitch the owner reported days later ("stuck in portrait after
 * going home") had no record left. The file survives that, and adb can read
 * it (shell is in ext_data_rw):
 *   adb shell cat /sdcard/Android/data/<package>/files/claudex-eventos.log
 */
object EventJournal {

    private const val TAG = "ClauDEX"
    private const val FILE_NAME = "claudex-eventos.log"
    private const val MAX_BYTES = 256 * 1024L

    private val writer = Executors.newSingleThreadExecutor()
    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, msg: String) {
        Log.i(TAG, msg)
        val app = context.applicationContext
        // one event per line: shell output inside a message ("svc data enable"
        // prints "enable: Success") broke lines and read as bogus events
        val line = "${stamp.format(Date())}|${msg.trim().replace('\n', ' ')}\n"
        writer.execute {
            try {
                val dir = app.getExternalFilesDir(null) ?: return@execute
                val f = File(dir, FILE_NAME)
                if (f.length() > MAX_BYTES) {
                    // keep the newest half instead of starting from nothing
                    val tail = f.readText().takeLast((MAX_BYTES / 2).toInt())
                    f.writeText(tail.substringAfter('\n'))
                }
                f.appendText(line)
            } catch (e: Exception) {}
        }
    }
}
