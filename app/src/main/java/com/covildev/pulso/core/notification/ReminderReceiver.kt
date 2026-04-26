package com.covildev.pulso.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.covildev.pulso.R

const val ACTION_REMINDER = "com.covildev.pulso.ACTION_REMINDER"
const val EXTRA_REMINDER_DAY = "extra_day"
const val EXTRA_REMINDER_HOUR = "extra_hour"
const val EXTRA_REMINDER_MINUTE = "extra_minute"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REMINDER) return

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, -1)
        val formattedTime = if (hour >= 0 && minute >= 0) {
            String.format("%02d:%02d", hour, minute)
        } else {
            ""
        }

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hora de medir sua pressão")
            .setContentText(
                if (formattedTime.isNotEmpty()) {
                    "Lembrete configurado para $formattedTime."
                } else {
                    "Não se esqueça de registrar sua medição."
                },
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
