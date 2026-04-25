package com.covildev.pulso.feature_metas.domain.repository

import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeGoals(): Flow<GoalSettings?>
    suspend fun getGoals(): GoalSettings?
    suspend fun saveGoals(goals: GoalSettings)
}
