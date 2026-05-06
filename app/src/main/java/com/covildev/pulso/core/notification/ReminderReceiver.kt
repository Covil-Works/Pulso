package com.covildev.pulso.core.notification

import android.Manifest
import android.app.AlarmManager
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
import com.covildev.pulso.feature_alarm.alert.AlarmAlertNotifier
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

const val ACTION_REMINDER = "com.covildev.pulso.ACTION_REMINDER"
const val EXTRA_REMINDER_DAY = "extra_day"
const val EXTRA_REMINDER_HOUR = "extra_hour"
const val EXTRA_REMINDER_MINUTE = "extra_minute"
const val EXTRA_REMINDER_NOTE = "extra_note"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REMINDER) return

        val canPostNotifications = !(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, -1)
        val dayOfWeek = intent.getIntExtra(EXTRA_REMINDER_DAY, -1)
        val note = intent.getStringExtra(EXTRA_REMINDER_NOTE)
        val formattedTime = if (hour >= 0 && minute >= 0) {
            String.format("%02d:%02d", hour, minute)
        } else {
            ""
        }

        if (canPostNotifications) {
            val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Hora de medir sua pressao")
                .setContentText(
                    if (formattedTime.isNotEmpty()) {
                        "Lembrete configurado para $formattedTime."
                    } else {
                        "Nao se esqueca de registrar sua medicao."
                    },
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        }

        if (hour >= 0 && minute >= 0 && dayOfWeek in 1..7) {
            AlarmAlertNotifier.show(
                context = context,
                hour = hour,
                minute = minute,
                dayLabel = dayLabel(dayOfWeek),
                note = note,
            )
            scheduleNextWeek(context, dayOfWeek, hour, minute, note)
        }
    }
}

private fun dayLabel(dayValue: Int): String = when (dayValue) {
    1 -> "Segunda"
    2 -> "Terca"
    3 -> "Quarta"
    4 -> "Quinta"
    5 -> "Sexta"
    6 -> "Sabado"
    7 -> "Domingo"
    else -> ""
}

private fun scheduleNextWeek(
    context: Context,
    dayValue: Int,
    hour: Int,
    minute: Int,
    note: String?,
) {
    val manager = context.getSystemService(AlarmManager::class.java) ?: return
    val localTime = LocalTime.of(hour, minute)
    val requestCode = dayValue * 10_000 + localTime.hour * 100 + localTime.minute
    val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
        action = ACTION_REMINDER
        putExtra(EXTRA_REMINDER_DAY, dayValue)
        putExtra(EXTRA_REMINDER_HOUR, hour)
        putExtra(EXTRA_REMINDER_MINUTE, minute)
        putExtra(EXTRA_REMINDER_NOTE, note)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        reminderIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val now = LocalDateTime.now()
    val triggerAt = now
        .with(TemporalAdjusters.next(DayOfWeek.of(dayValue)))
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    runCatching {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
