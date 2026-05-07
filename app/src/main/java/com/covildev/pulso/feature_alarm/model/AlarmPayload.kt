package com.covildev.pulso.feature_alarm.model

import android.content.Intent
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
const val EXTRA_ALARM_DAY = "extra_alarm_day"
const val EXTRA_ALARM_TITLE = "extra_alarm_title"
const val EXTRA_ALARM_NOTE = "extra_alarm_note"
const val EXTRA_ALARM_TYPE = "extra_alarm_type"

private val DEFAULT_LOCALE: Locale = Locale.forLanguageTag("pt-BR")

data class AlarmPayload(
    val hour: Int,
    val minute: Int,
    val dayOfWeek: Int,
    val title: String,
    val note: String? = null,
    val type: AlarmType = AlarmType.OTHER,
) {
    val formattedTime: String
        get() = String.format(DEFAULT_LOCALE, "%02d:%02d", hour, minute)

    val dayLabel: String
        get() {
            val dayName = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, DEFAULT_LOCALE)
            return dayName.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(DEFAULT_LOCALE) else first.toString()
            }
        }

    val normalizedNote: String?
        get() = note?.trim()?.takeIf { it.isNotEmpty() }

    val requestCode: Int
        get() = type.stableCode * 1_000_000 + dayOfWeek * 10_000 + hour * 100 + minute

    val notificationId: Int
        get() = 99_000 + requestCode
}

fun Intent.putAlarmPayload(payload: AlarmPayload): Intent {
    return apply {
        putExtra(EXTRA_ALARM_HOUR, payload.hour)
        putExtra(EXTRA_ALARM_MINUTE, payload.minute)
        putExtra(EXTRA_ALARM_DAY, payload.dayOfWeek)
        putExtra(EXTRA_ALARM_TITLE, payload.title)
        putExtra(EXTRA_ALARM_NOTE, payload.normalizedNote)
        putExtra(EXTRA_ALARM_TYPE, payload.type.rawValue)
    }
}

fun Intent.getAlarmPayload(): AlarmPayload? {
    val hour = getIntExtra(EXTRA_ALARM_HOUR, -1)
    val minute = getIntExtra(EXTRA_ALARM_MINUTE, -1)
    val dayOfWeek = getIntExtra(EXTRA_ALARM_DAY, -1)
    val title = getStringExtra(EXTRA_ALARM_TITLE).orEmpty().trim()

    if (hour !in 0..23 || minute !in 0..59 || dayOfWeek !in 1..7 || title.isEmpty()) {
        return null
    }

    return AlarmPayload(
        hour = hour,
        minute = minute,
        dayOfWeek = dayOfWeek,
        title = title,
        note = getStringExtra(EXTRA_ALARM_NOTE),
        type = AlarmType.fromRawValue(getStringExtra(EXTRA_ALARM_TYPE)),
    )
}
