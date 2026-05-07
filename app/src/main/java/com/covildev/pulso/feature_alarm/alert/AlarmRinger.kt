package com.covildev.pulso.feature_alarm.alert

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AlarmRinger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var toneJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    fun start(context: Context) {
        if (toneJob?.isActive == true) return

        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneJob = scope.launch {
            while (isActive) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                delay(1_000)
            }
        }

        startVibration(context)
    }

    fun stop(context: Context) {
        toneJob?.cancel()
        toneJob = null
        toneGenerator?.release()
        toneGenerator = null
        stopVibration(context)
    }

    private fun startVibration(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val vibrationPattern = longArrayOf(0, 600, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, 0)
        }
    }

    private fun stopVibration(context: Context) {
        val vibrator = getVibrator(context) ?: return
        vibrator.cancel()
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }
}
