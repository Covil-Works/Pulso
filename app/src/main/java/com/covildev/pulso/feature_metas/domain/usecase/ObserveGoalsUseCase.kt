package com.covildev.pulso.feature_metas.domain.usecase

import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import com.covildev.pulso.feature_metas.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(): Flow<GoalSettings?> = repository.observeGoals()
}
