package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.data.repository.ScannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires ~65 seconds after a directional scan and posts a notification asking
 * the user to mark the trade outcome (WIN/LOSS), keeping audit stats accurate.
 */
class OutcomeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scanId = intent.getLongExtra(NotificationHelper.EXTRA_SCAN_ID, -1L)
        val prediction = intent.getStringExtra(NotificationHelper.EXTRA_PREDICTION) ?: "UP"
        if (scanId <= 0L) return

        NotificationHelper.createReminderChannel(context)
        val notification = NotificationHelper.buildOutcomeReminder(context, scanId, prediction)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NotificationHelper.reminderNotificationId(scanId), notification)
    }

    companion object {
        fun schedule(context: Context, scanId: Long, prediction: String, delayMs: Long = 65_000L) {
            val intent = Intent(context, OutcomeReminderReceiver::class.java).apply {
                putExtra(NotificationHelper.EXTRA_SCAN_ID, scanId)
                putExtra(NotificationHelper.EXTRA_PREDICTION, prediction)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scanId.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Inexact alarm: no SCHEDULE_EXACT_ALARM permission needed.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pendingIntent
            )
        }
    }
}

/**
 * Handles the WIN / LOSS action buttons on the outcome reminder notification.
 */
class OutcomeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scanId = intent.getLongExtra(NotificationHelper.EXTRA_SCAN_ID, -1L)
                val outcome = intent.getStringExtra(NotificationHelper.EXTRA_OUTCOME)
                if (scanId > 0L && (outcome == "WIN" || outcome == "LOSS")) {
                    ScannerRepository.getInstance(context).updateOutcome(scanId, outcome)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NotificationHelper.reminderNotificationId(scanId))
            } catch (ignored: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
