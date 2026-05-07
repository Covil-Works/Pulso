package com.covildev.pulso.core.runtime

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

private const val TRACE_TAG = "AlarmTrace"

object AppForegroundTracker {
    private val startedActivitiesCount = AtomicInteger(0)
    @Volatile
    private var initialized = false

    @Volatile
    var isForeground: Boolean = false
        private set

    fun init(application: Application) {
        if (initialized) return
        initialized = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) {
                    val count = startedActivitiesCount.incrementAndGet()
                    isForeground = count > 0
                    Log.d(TRACE_TAG, "App foreground update onStart count=$count")
                }

                override fun onActivityResumed(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) {
                    val count = startedActivitiesCount.decrementAndGet().coerceAtLeast(0)
                    if (count == 0) {
                        startedActivitiesCount.set(0)
                    }
                    isForeground = count > 0
                    Log.d(TRACE_TAG, "App foreground update onStop count=$count")
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
