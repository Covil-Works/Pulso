package com.covildev.pulso.feature_alarm.ui

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.covildev.pulso.feature_alarm.alert.AlarmAlertService
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.feature_alarm.model.getAlarmPayload
import com.covildev.pulso.feature_alarm.model.putAlarmPayload
import com.covildev.pulso.ui.theme.PulsoTheme

class AlarmAlertActivity : ComponentActivity() {
    private var currentAlarmPayload: AlarmPayload? by mutableStateOf(null)

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
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentAlarmPayload = intent.getAlarmPayload()
        Log.i(TAG, "onCreate payloadReq=${currentAlarmPayload?.requestCode} sdk=${Build.VERSION.SDK_INT}")
        if (currentAlarmPayload == null) {
            Log.w(TAG, "onCreate sem payload valido. Finalizando activity.")
            finish()
            return
        }

        setContent {
            PulsoTheme {
                currentAlarmPayload?.let { payload ->
                    AlarmAlertScreen(
                        alarmPayload = payload,
                        onDismiss = ::dismissAlarm,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentAlarmPayload = intent.getAlarmPayload() ?: currentAlarmPayload
        Log.i(TAG, "onNewIntent payloadReq=${currentAlarmPayload?.requestCode}")
    }

    private fun dismissAlarm() {
        Log.i(TAG, "dismissAlarm payloadReq=${currentAlarmPayload?.requestCode}")
        AlarmAlertService.stop(applicationContext)
        finish()
    }

    companion object {
        private const val TAG = "AlarmAlertActivity"

        private fun backgroundActivityStartMode(): Int {
            return if (Build.VERSION.SDK_INT >= 36) {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            } else {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
        }

        private fun createBackgroundLaunchCreatorOptions(): Bundle? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return null
            }
            return ActivityOptions.makeBasic()
                .setPendingIntentCreatorBackgroundActivityStartMode(
                    backgroundActivityStartMode(),
                )
                .toBundle()
        }

        private fun createBackgroundLaunchSenderOptions(): Bundle? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return null
            }
            return ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    backgroundActivityStartMode(),
                )
                .toBundle()
        }

        fun createIntent(
            context: Context,
            alarmPayload: AlarmPayload,
        ): Intent {
            return Intent(context, AlarmAlertActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                .putAlarmPayload(alarmPayload)
        }

        fun createLaunchPendingIntent(
            context: Context,
            alarmPayload: AlarmPayload,
        ): PendingIntent {
            return PendingIntent.getActivity(
                context,
                alarmPayload.requestCode,
                createIntent(
                    context = context,
                    alarmPayload = alarmPayload,
                ),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                createBackgroundLaunchCreatorOptions(),
            )
        }

        fun launchNow(
            context: Context,
            alarmPayload: AlarmPayload,
        ): Boolean {
            Log.i(TAG, "launchNow start req=${alarmPayload.requestCode} sdk=${Build.VERSION.SDK_INT}")
            val pendingIntent = createLaunchPendingIntent(
                context = context,
                alarmPayload = alarmPayload,
            )
            val sent = runCatching {
                pendingIntent.send(
                    context,
                    0,
                    null,
                    null,
                    null,
                    null,
                    createBackgroundLaunchSenderOptions(),
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Falha ao enviar PendingIntent para abrir alarme imediatamente.", throwable)
            }.onSuccess {
                Log.i(TAG, "PendingIntent enviado com sucesso req=${alarmPayload.requestCode}")
            }.isSuccess
            if (!sent) {
                return runCatching {
                    context.startActivity(
                        createIntent(
                            context = context,
                            alarmPayload = alarmPayload,
                        ),
                    )
                    true
                }.onFailure { throwable ->
                    Log.w(TAG, "Falha ao abrir AlarmAlertActivity diretamente.", throwable)
                }.onSuccess {
                    Log.i(TAG, "Fallback startActivity executado req=${alarmPayload.requestCode}")
                }.getOrDefault(false)
            }
            return true
        }

        fun launchFromForeground(
            context: Context,
            alarmPayload: AlarmPayload,
        ): Boolean {
            Log.i(TAG, "launchFromForeground start req=${alarmPayload.requestCode}")
            return runCatching {
                context.startActivity(
                    createIntent(
                        context = context,
                        alarmPayload = alarmPayload,
                    ),
                )
                true
            }.onFailure { throwable ->
                Log.w(TAG, "Falha ao abrir AlarmAlertActivity em foreground.", throwable)
            }.onSuccess {
                Log.i(TAG, "launchFromForeground sucesso req=${alarmPayload.requestCode}")
            }.getOrDefault(false)
        }
    }
}
