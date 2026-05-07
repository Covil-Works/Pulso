package com.covildev.pulso

import android.app.Application
import com.covildev.pulso.core.notification.createReminderNotificationChannel
import com.covildev.pulso.core.runtime.AppForegroundTracker
import com.covildev.pulso.feature_alarm.alert.createAlarmAlertNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PulsoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppForegroundTracker.init(this)
        createReminderNotificationChannel(this)
        createAlarmAlertNotificationChannel(this)
    }
}
