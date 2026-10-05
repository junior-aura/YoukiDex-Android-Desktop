package com.youki.dex.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.youki.dex.R

/**
 * ClauDEX: a lasting notice while Shizuku is down, gone when it is back.
 *
 * Measured on a SM-A055M: the Shizuku server is a child of adbd - same cgroup
 * (0::/uid_0/pid_<adbd>), oom_score_adj -1000 - so memory pressure never takes
 * it, but every adbd restart does (`adb tcpip`, toggling a debugging setting).
 * Nothing restarts it short of a reboot: once it stayed down four days, the
 * window features silently off; a toast on its death was easy to miss, and
 * none came at all when this service itself had restarted meanwhile.
 *
 * Tapping opens Shizuku: with wireless debugging on, its Start brings the
 * server back in a tap. The notice is ongoing, so the badge-only dock does not
 * count it as news.
 */
object ShizukuNotice {
    private const val CHANNEL = "claudex_shizuku"
    private const val ID = 7301
    private const val SHIZUKU = "moe.shizuku.privileged.api"

    fun show(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.shizuku_notice_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = context.packageManager.getLaunchIntentForPackage(SHIZUKU)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { PendingIntent.getActivity(context, ID, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_dock)
            .setContentTitle(context.getString(R.string.shizuku_notice_title))
            .setContentText(context.getString(R.string.shizuku_notice_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.shizuku_notice_text)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply { if (open != null) setContentIntent(open) }
            .build()
        try { nm.notify(ID, n) } catch (e: Exception) {}
    }

    fun hide(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID)
    }
}
