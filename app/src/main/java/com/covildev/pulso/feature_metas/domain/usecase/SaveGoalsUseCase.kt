package com.covildev.pulso.feature_metas.domain.usecase

import com.covildev.pulso.core.notification.ReminderScheduler
import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import com.covildev.pulso.feature_metas.domain.repository.GoalRepository
import dagger.Lazy
import javax.inject.Inject

class SaveGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
    private val reminderScheduler: Lazy<ReminderScheduler>,
) {
    suspend operator fun invoke(goals: GoalSettings): Result<Unit> {
        val sanitizedGoals = GoalSettings(
            daysOfWeek = goals.daysOfWeek.filter { it in 1..7 }.toSet(),
            timesOfDay = goals.timesOfDay.distinct().sorted(),
        )

        if (sanitizedGoals.daysOfWeek.isEmpty()) {
            return Result.failure(IllegalArgumentException("Selecione ao menos um dia da semana."))
        }
        if (sanitizedGoals.timesOfDay.isEmpty()) {
            return Result.failure(IllegalArgumentException("Selecione ao menos um horario."))
        }

        return runCatching {
            val scheduler = reminderScheduler.get()
            val previousGoals = repository.getGoals()
            previousGoals?.let { scheduler.cancel(it) }
            repository.saveGoals(sanitizedGoals)
            scheduler.schedule(sanitizedGoals)
        }
    }
}
