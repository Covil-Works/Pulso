package com.covildev.pulso.feature_alarm.alert

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.covildev.pulso.R
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.feature_alarm.ui.AlarmAlertActivity

const val ALARM_ALERT_CHANNEL_ID = "app_alarm_alerts"
private const val TRACE_TAG = "AlarmTrace"

object AlarmAlertNotifier {
    fun buildNotification(
        context: Context,
        alarmPayload: AlarmPayload,
    ): Notification {
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val vibrationPattern = longArrayOf(0, 1200, 400, 1200, 400, 1200)
        val fullScreenPendingIntent = AlarmAlertActivity.createLaunchPendingIntent(
            context = context,
            alarmPayload = alarmPayload,
        )
        val stopPendingIntent = AlarmAlertService.createStopPendingIntent(
            context = context,
            requestCode = alarmPayload.requestCode,
        )
        Log.d(
            TRACE_TAG,
            "buildNotification req=${alarmPayload.requestCode} notif=${alarmPayload.notificationId} channel=$ALARM_ALERT_CHANNEL_ID",
        )

        return NotificationCompat.Builder(context, ALARM_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alarmPayload.title)
            .setContentText(
                alarmPayload.normalizedNote ?: "${alarmPayload.dayLabel} - ${alarmPayload.formattedTime}",
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(alarmSoundUri)
            .setVibrate(vibrationPattern)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                0,
                "Abrir alarme",
                fullScreenPendingIntent,
            )
            .addAction(
                0,
                "Encerrar",
                stopPendingIntent,
            )
            .build()
    }
}

fun createAlarmAlertNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = android.app.NotificationChannel(
        ALARM_ALERT_CHANNEL_ID,
        "Alarmes do aplicativo",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        description = "Canal para alarmes em tela cheia"
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setSound(alarmSoundUri, alarmAudioAttributes)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 1200, 400, 1200, 400, 1200)
    }
    manager.createNotificationChannel(channel)
    Log.i(TRACE_TAG, "Canal criado/atualizado id=$ALARM_ALERT_CHANNEL_ID importance=${channel.importance}")
}
