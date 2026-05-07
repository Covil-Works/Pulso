package com.covildev.pulso.feature_alarm.alert

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.feature_alarm.model.getAlarmPayload
import com.covildev.pulso.feature_alarm.model.putAlarmPayload

private const val ACTION_START_ALARM = "com.covildev.pulso.feature_alarm.START"
private const val ACTION_STOP_ALARM = "com.covildev.pulso.feature_alarm.STOP"

class AlarmAlertService : Service() {
    private var activeAlarm: AlarmPayload? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action} startId=$startId active=${activeAlarm?.requestCode}")
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val payload = intent.getAlarmPayload() ?: run {
                    Log.w(TAG, "ACTION_START_ALARM sem payload valido. Encerrando service.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (activeAlarm != null && activeAlarm != payload) {
                    Log.i(TAG, "Substituindo alarme ativo old=${activeAlarm?.requestCode} new=${payload.requestCode}")
                    stopAlarm()
                }
                activeAlarm = payload
                Log.i(TAG, "Iniciando alarme req=${payload.requestCode} notif=${payload.notificationId}")
                AlarmRinger.start(applicationContext)
                val notification = AlarmAlertNotifier.buildNotification(
                    context = applicationContext,
                    alarmPayload = payload,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        payload.notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                    )
                } else {
                    startForeground(payload.notificationId, notification)
                }
                Log.i(TAG, "startForeground concluido req=${payload.requestCode}")
            }

            ACTION_STOP_ALARM -> {
                Log.i(TAG, "Recebido ACTION_STOP_ALARM")
                stopAlarmAndSelf()
            }

            else -> {
                Log.w(TAG, "Action inesperada no service: ${intent?.action}. Encerrando.")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy service")
        stopAlarm()
        super.onDestroy()
    }

    private fun stopAlarmAndSelf() {
        stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAlarm() {
        val payload = activeAlarm
        activeAlarm = null
        AlarmRinger.stop(applicationContext)
        if (payload != null) {
            NotificationManagerCompat.from(applicationContext).cancel(payload.notificationId)
            Log.i(TAG, "Alarme parado req=${payload.requestCode} notif=${payload.notificationId}")
        } else {
            Log.d(TAG, "stopAlarm chamado sem alarme ativo.")
        }
    }

    companion object {
        private const val TAG = "AlarmAlertService"

        fun start(context: Context, alarmPayload: AlarmPayload): Boolean {
            val intent = Intent(context, AlarmAlertService::class.java)
                .setAction(ACTION_START_ALARM)
                .putAlarmPayload(alarmPayload)
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "Solicitado start do service req=${alarmPayload.requestCode}")
                true
            }.onFailure { throwable ->
                Log.w(TAG, "Falha ao iniciar servico de alarme em foreground.", throwable)
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmAlertService::class.java).setAction(ACTION_STOP_ALARM)
            runCatching {
                context.startService(intent)
            }.onFailure { throwable ->
                Log.w(TAG, "Falha ao solicitar parada do servico de alarme.", throwable)
            }
        }

        fun createStopPendingIntent(context: Context, requestCode: Int): PendingIntent {
            val stopIntent = Intent(context, AlarmAlertService::class.java).setAction(ACTION_STOP_ALARM)
            return PendingIntent.getService(
                context,
                requestCode,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
