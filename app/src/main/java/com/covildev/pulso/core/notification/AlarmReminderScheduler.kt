package com.covildev.pulso.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.feature_alarm.model.AlarmType
import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

private const val GOALS_ALARM_TITLE = "Est\u00E1 na hora de medir sua press\u00E3o"
private const val TRACE_TAG = "AlarmTrace"

class AlarmReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderScheduler {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    override fun schedule(goals: GoalSettings) {
        val manager = alarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            Log.w(TRACE_TAG, "Schedule ignorado: sem permissao de exact alarm.")
            return
        }
        val validDays = goals.daysOfWeek.filter { it in 1..7 }
        Log.i(TRACE_TAG, "Schedule iniciado days=${validDays.size} times=${goals.timesOfDay.distinct().size}")
        validDays.forEach { dayValue ->
            goals.timesOfDay.distinct().forEach { localTime ->
                val alarmPayload = buildGoalsAlarmPayload(
                    dayValue = dayValue,
                    localTime = localTime,
                    note = goals.alarmNote,
                )
                val pendingIntent = buildPendingIntent(alarmPayload)
                runCatching {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis(
                            dayValue = dayValue,
                            localTime = localTime,
                        ),
                        pendingIntent,
                    )
                }.onSuccess {
                    Log.i(
                        TRACE_TAG,
                        "Alarm agendado req=${alarmPayload.requestCode} day=$dayValue time=${alarmPayload.formattedTime}",
                    )
                }.onFailure { throwable ->
                    Log.e(
                        TRACE_TAG,
                        "Falha ao agendar req=${alarmPayload.requestCode} day=$dayValue time=${alarmPayload.formattedTime}",
                        throwable,
                    )
                }
            }
        }
    }

    override fun cancel(goals: GoalSettings) {
        val manager = alarmManager ?: return
        val validDays = goals.daysOfWeek.filter { it in 1..7 }
        Log.i(TRACE_TAG, "Cancel iniciado days=${validDays.size} times=${goals.timesOfDay.distinct().size}")
        validDays.forEach { dayValue ->
            goals.timesOfDay.distinct().forEach { localTime ->
                val alarmPayload = buildGoalsAlarmPayload(
                    dayValue = dayValue,
                    localTime = localTime,
                    note = goals.alarmNote,
                )
                val pendingIntent = buildPendingIntent(alarmPayload)
                runCatching {
                    manager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }.onSuccess {
                    Log.i(
                        TRACE_TAG,
                        "Alarm cancelado req=${alarmPayload.requestCode} day=$dayValue time=${alarmPayload.formattedTime}",
                    )
                }.onFailure { throwable ->
                    Log.e(
                        TRACE_TAG,
                        "Falha ao cancelar req=${alarmPayload.requestCode} day=$dayValue time=${alarmPayload.formattedTime}",
                        throwable,
                    )
                }
            }
        }
    }

    private fun nextTriggerMillis(dayValue: Int, localTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val targetDay = DayOfWeek.of(dayValue)
        var next = now
            .with(TemporalAdjusters.nextOrSame(targetDay))
            .withHour(localTime.hour)
            .withMinute(localTime.minute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun buildPendingIntent(
        alarmPayload: AlarmPayload,
    ): PendingIntent {
        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_DAY, alarmPayload.dayOfWeek)
            putExtra(EXTRA_REMINDER_HOUR, alarmPayload.hour)
            putExtra(EXTRA_REMINDER_MINUTE, alarmPayload.minute)
            putExtra(EXTRA_REMINDER_NOTE, alarmPayload.normalizedNote)
            putExtra(EXTRA_REMINDER_TITLE, alarmPayload.title)
            putExtra(EXTRA_REMINDER_TYPE, alarmPayload.type.rawValue)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmPayload.requestCode,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildGoalsAlarmPayload(
        dayValue: Int,
        localTime: LocalTime,
        note: String?,
    ): AlarmPayload {
        return AlarmPayload(
            hour = localTime.hour,
            minute = localTime.minute,
            dayOfWeek = dayValue,
            title = GOALS_ALARM_TITLE,
            note = note,
            type = AlarmType.BLOOD_PRESSURE,
        )
    }
}
