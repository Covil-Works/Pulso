package com.covildev.pulso.core.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.covildev.pulso.R
import com.covildev.pulso.feature_alarm.alert.ALARM_ALERT_CHANNEL_ID
import com.covildev.pulso.feature_alarm.alert.AlarmAlertService
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.feature_alarm.model.AlarmType
import com.covildev.pulso.feature_alarm.ui.AlarmAlertActivity
import com.covildev.pulso.core.runtime.AppForegroundTracker
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
const val EXTRA_REMINDER_TITLE = "extra_title"
const val EXTRA_REMINDER_TYPE = "extra_type"

private const val DEFAULT_BLOOD_PRESSURE_TITLE = "Est\u00E1 na hora de medir sua press\u00E3o"
private const val TRACE_TAG = "AlarmTrace"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REMINDER) {
            Log.d(TRACE_TAG, "Receiver ignorou intent action=${intent?.action}")
            return
        }

        val payload = intent.toAlarmPayload()
        if (payload == null) {
            Log.w(TRACE_TAG, "Receiver recebeu extras invalidos e descartou o alarme.")
            return
        }
        val hasFullScreenPermission = canUseFullScreenIntent(context)
        val isAppForeground = AppForegroundTracker.isForeground
        Log.i(
            TRACE_TAG,
            "Receiver disparado payload=${payload.traceSummary()} fullScreenPerm=$hasFullScreenPermission foreground=$isAppForeground sdk=${Build.VERSION.SDK_INT}",
        )
        val serviceStarted = AlarmAlertService.start(
            context = context,
            alarmPayload = payload,
        )
        Log.i(TRACE_TAG, "Resultado start service payload=${payload.traceSummary()} started=$serviceStarted")
        var launchTriggered = false
        if (isAppForeground) {
            launchTriggered = AlarmAlertActivity.launchFromForeground(
                context = context,
                alarmPayload = payload,
            )
            Log.i(
                TRACE_TAG,
                "Tentativa launchFromForeground payload=${payload.traceSummary()} launched=$launchTriggered",
            )
        }
        if (!launchTriggered && hasFullScreenPermission) {
            launchTriggered = AlarmAlertActivity.launchNow(
                context = context,
                alarmPayload = payload,
            )
            Log.i(TRACE_TAG, "Tentativa launchNow payload=${payload.traceSummary()} sent=$launchTriggered")
        }
        if (!serviceStarted) {
            Log.w(TRACE_TAG, "Fallback compat notification por falha no service payload=${payload.traceSummary()}")
            showCompatibilityNotification(
                context = context,
                payload = payload,
                reason = CompatibilityReason.SERVICE_START_FAILED,
            )
        } else if (!hasFullScreenPermission) {
            Log.w(
                TRACE_TAG,
                "Fallback compat notification por falta de full-screen permission payload=${payload.traceSummary()}",
            )
            showCompatibilityNotification(
                context = context,
                payload = payload,
                reason = CompatibilityReason.FULL_SCREEN_PERMISSION_MISSING,
            )
        }
        Log.d(TRACE_TAG, "Reagendando proxima semana payload=${payload.traceSummary()}")
        scheduleNextWeek(
            context = context,
            payload = payload,
        )
    }
}

private fun Intent.toAlarmPayload(): AlarmPayload? {
    val hour = getIntExtra(EXTRA_REMINDER_HOUR, -1)
    val minute = getIntExtra(EXTRA_REMINDER_MINUTE, -1)
    val dayOfWeek = getIntExtra(EXTRA_REMINDER_DAY, -1)
    if (hour !in 0..23 || minute !in 0..59 || dayOfWeek !in 1..7) {
        return null
    }

    return AlarmPayload(
        hour = hour,
        minute = minute,
        dayOfWeek = dayOfWeek,
        title = getStringExtra(EXTRA_REMINDER_TITLE)?.trim().takeUnless { it.isNullOrEmpty() }
            ?: DEFAULT_BLOOD_PRESSURE_TITLE,
        note = getStringExtra(EXTRA_REMINDER_NOTE),
        type = AlarmType.fromRawValue(getStringExtra(EXTRA_REMINDER_TYPE) ?: AlarmType.BLOOD_PRESSURE.rawValue),
    )
}

private fun showCompatibilityNotification(
    context: Context,
    payload: AlarmPayload,
    reason: CompatibilityReason,
) {
    val canPostNotifications = !(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        )
    if (!canPostNotifications) {
        Log.w(TRACE_TAG, "Sem permissao de notificacao para fallback payload=${payload.traceSummary()}")
        return
    }

    val launchPendingIntent = AlarmAlertActivity.createLaunchPendingIntent(
        context = context,
        alarmPayload = payload,
    )

    val contentText = when (reason) {
        CompatibilityReason.FULL_SCREEN_PERMISSION_MISSING ->
            "Ative \"Exibição em tela cheia\" para abrir o alarme automaticamente."
        CompatibilityReason.SERVICE_START_FAILED ->
            payload.normalizedNote ?: "Lembrete configurado para ${payload.formattedTime}."
    }
    val notificationBuilder = NotificationCompat.Builder(context, ALARM_ALERT_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(payload.title)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(launchPendingIntent)

    val notification = notificationBuilder.build()

    NotificationManagerCompat.from(context).notify(
        payload.notificationId + 1_000_000,
        notification,
    )
    Log.i(
        TRACE_TAG,
        "Fallback notification publicada payload=${payload.traceSummary()} reason=$reason channel=$ALARM_ALERT_CHANNEL_ID",
    )
}

private fun scheduleNextWeek(
    context: Context,
    payload: AlarmPayload,
) {
    val manager = context.getSystemService(AlarmManager::class.java) ?: return
    val localTime = LocalTime.of(payload.hour, payload.minute)
    val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
        action = ACTION_REMINDER
        putExtra(EXTRA_REMINDER_DAY, payload.dayOfWeek)
        putExtra(EXTRA_REMINDER_HOUR, payload.hour)
        putExtra(EXTRA_REMINDER_MINUTE, payload.minute)
        putExtra(EXTRA_REMINDER_NOTE, payload.normalizedNote)
        putExtra(EXTRA_REMINDER_TITLE, payload.title)
        putExtra(EXTRA_REMINDER_TYPE, payload.type.rawValue)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        payload.requestCode,
        reminderIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val now = LocalDateTime.now()
    val triggerAt = now
        .with(TemporalAdjusters.next(DayOfWeek.of(payload.dayOfWeek)))
        .withHour(localTime.hour)
        .withMinute(localTime.minute)
        .withSecond(0)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    runCatching {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }.onSuccess {
        Log.i(
            TRACE_TAG,
            "Reagendado payload=${payload.traceSummary()} triggerAtEpochMs=$triggerAt",
        )
    }.onFailure { throwable ->
        Log.e(
            TRACE_TAG,
            "Falha ao reagendar payload=${payload.traceSummary()} triggerAtEpochMs=$triggerAt",
            throwable,
        )
    }
}

private fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    return notificationManager?.canUseFullScreenIntent() == true
}

private enum class CompatibilityReason {
    FULL_SCREEN_PERMISSION_MISSING,
    SERVICE_START_FAILED,
}

private fun AlarmPayload.traceSummary(): String {
    return "req=$requestCode notif=$notificationId day=$dayOfWeek time=$formattedTime type=${type.rawValue}"
}
