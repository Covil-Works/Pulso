package com.covildev.pulso.core.notification

import com.covildev.pulso.feature_metas.domain.model.GoalSettings

interface ReminderScheduler {
    fun schedule(goals: GoalSettings)
    fun cancel(goals: GoalSettings)
}
