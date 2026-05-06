package com.covildev.pulso.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class AlarmReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderScheduler {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    override fun schedule(goals: GoalSettings) {
        val manager = alarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            return
        }
        val validDays = goals.daysOfWeek.filter { it in 1..7 }
        validDays.forEach { dayValue ->
            goals.timesOfDay.distinct().forEach { localTime ->
                val requestCode = requestCodeFor(dayOfWeek = dayValue, localTime = localTime)
                val pendingIntent = buildPendingIntent(dayValue, localTime, requestCode, goals.alarmNote)
                runCatching {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis(dayValue = dayValue, localTime = localTime),
                        pendingIntent,
                    )
                }
            }
        }
    }

    override fun cancel(goals: GoalSettings) {
        val manager = alarmManager ?: return
        val validDays = goals.daysOfWeek.filter { it in 1..7 }
        validDays.forEach { dayValue ->
            goals.timesOfDay.distinct().forEach { localTime ->
                val requestCode = requestCodeFor(dayOfWeek = dayValue, localTime = localTime)
                val pendingIntent = buildPendingIntent(dayValue, localTime, requestCode, goals.alarmNote)
                runCatching {
                    manager.cancel(pendingIntent)
                    pendingIntent.cancel()
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
        dayValue: Int,
        localTime: LocalTime,
        requestCode: Int,
        note: String?,
    ): PendingIntent {
        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_DAY, dayValue)
            putExtra(EXTRA_REMINDER_HOUR, localTime.hour)
            putExtra(EXTRA_REMINDER_MINUTE, localTime.minute)
            putExtra(EXTRA_REMINDER_NOTE, note)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private fun requestCodeFor(dayOfWeek: Int, localTime: LocalTime): Int {
    return dayOfWeek * 10_000 + localTime.hour * 100 + localTime.minute
}
