package com.covildev.pulso

import android.app.Application
import com.covildev.pulso.core.notification.createReminderNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PulsoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
    }
}
