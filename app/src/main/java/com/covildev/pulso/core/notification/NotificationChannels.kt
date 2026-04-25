package com.covildev.pulso.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val REMINDER_CHANNEL_ID = "pressure_reminders"
private const val REMINDER_CHANNEL_NAME = "Lembretes de medicao"
private const val REMINDER_CHANNEL_DESCRIPTION = "Notificacoes para registrar a pressao arterial"

fun createReminderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        REMINDER_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = REMINDER_CHANNEL_DESCRIPTION
    }
    manager.createNotificationChannel(channel)
}
