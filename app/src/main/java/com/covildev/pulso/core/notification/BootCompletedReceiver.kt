package com.covildev.pulso.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.covildev.pulso.feature_metas.domain.repository.GoalRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var goalRepository: GoalRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            goalRepository.getGoals()?.let { reminderScheduler.schedule(it) }
            pendingResult.finish()
        }
    }
}
