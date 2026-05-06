package com.covildev.pulso.feature_alarm.alert

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.covildev.pulso.R
import com.covildev.pulso.feature_alarm.ui.AlarmAlertActivity

const val ALARM_ALERT_CHANNEL_ID = "pressure_alarm_alert"

object AlarmAlertNotifier {
    fun show(
        context: Context,
        hour: Int,
        minute: Int,
        dayLabel: String,
        note: String?,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fullScreenIntent = AlarmAlertActivity.createIntent(
            context = context,
            hour = hour,
            minute = minute,
            dayLabel = dayLabel,
            note = note,
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            buildRequestCode(hour, minute),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ALARM_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alarme de pressão")
            .setContentText("Você deve medir a sua pressão agora.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            buildNotificationId(hour, minute),
            notification,
        )
    }
}

fun createAlarmAlertNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = android.app.NotificationChannel(
        ALARM_ALERT_CHANNEL_ID,
        "Alarmes de pressão",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Canal para alarme em tela cheia de medição da pressão"
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }
    manager.createNotificationChannel(channel)
}

private fun buildRequestCode(hour: Int, minute: Int): Int = hour * 100 + minute

private fun buildNotificationId(hour: Int, minute: Int): Int = 99_000 + buildRequestCode(hour, minute)
