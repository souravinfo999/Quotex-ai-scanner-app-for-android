package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "quotex_scanner_channel"
    const val CHANNEL_REMINDERS = "quotex_outcome_reminders"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"
    const val ACTION_MARK_OUTCOME = "com.example.service.ACTION_MARK_OUTCOME"
    const val EXTRA_SCAN_ID = "extra_scan_id"
    const val EXTRA_PREDICTION = "extra_prediction"
    const val EXTRA_OUTCOME = "extra_outcome"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quotex AI Scanner Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of Quotex AI Pro floating overlay scanner"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Trade Outcome Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to mark WIN/LOSS after each signal's 1-minute expiry"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun reminderNotificationId(scanId: Long): Int = 2000 + (scanId % 100000).toInt()

    /**
     * Builds the "mark your trade outcome" notification with WIN / LOSS buttons.
     */
    fun buildOutcomeReminder(context: Context, scanId: Long, prediction: String): Notification {
        val direction = if (prediction.equals("UP", ignoreCase = true)) "CALL (UP) 📈" else "PUT (DOWN) 📉"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminderNotificationId(scanId),
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val winIntent = Intent(context, OutcomeActionReceiver::class.java).apply {
            action = ACTION_MARK_OUTCOME
            putExtra(EXTRA_SCAN_ID, scanId)
            putExtra(EXTRA_OUTCOME, "WIN")
        }
        val winPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderNotificationId(scanId) + 500000,
            winIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val lossIntent = Intent(context, OutcomeActionReceiver::class.java).apply {
            action = ACTION_MARK_OUTCOME
            putExtra(EXTRA_SCAN_ID, scanId)
            putExtra(EXTRA_OUTCOME, "LOSS")
        }
        val lossPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderNotificationId(scanId) + 600000,
            lossIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle("⏰ Trade expired — mark result")
            .setContentText("Your $direction signal finished. Was it a WIN or LOSS?")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✅ WIN", winPendingIntent)
            .addAction(android.R.drawable.ic_delete, "❌ LOSS", lossPendingIntent)
            .build()
    }

    fun buildForegroundNotification(context: Context): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🤖 Quotex AI Pro Scanner Running")
            .setContentText("Tap floating button to analyze chart")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open App", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
            .build()
    }
}
