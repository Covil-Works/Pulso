package com.covildev.pulso.feature_alarm.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.covildev.pulso.ui.theme.PulsoTheme

const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
const val EXTRA_ALARM_DAY = "extra_alarm_day"
const val EXTRA_ALARM_NOTE = "extra_alarm_note"

class AlarmAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val dayLabel = intent.getStringExtra(EXTRA_ALARM_DAY).orEmpty()
        val note = intent.getStringExtra(EXTRA_ALARM_NOTE)

        setContent {
            PulsoTheme {
                AlarmAlertScreen(
                    hour = hour,
                    minute = minute,
                    dayLabel = dayLabel,
                    note = note,
                    onDismiss = ::finish,
                )
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            hour: Int,
            minute: Int,
            dayLabel: String,
            note: String?,
        ): Intent {
            return Intent(context, AlarmAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_ALARM_HOUR, hour)
                putExtra(EXTRA_ALARM_MINUTE, minute)
                putExtra(EXTRA_ALARM_DAY, dayLabel)
                putExtra(EXTRA_ALARM_NOTE, note)
            }
        }
    }
}
