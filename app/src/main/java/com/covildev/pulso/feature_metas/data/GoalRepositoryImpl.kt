package com.covildev.pulso.feature_metas.data

import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import com.covildev.pulso.feature_metas.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao,
) : GoalRepository {
    override fun observeGoals(): Flow<GoalSettings?> {
        return dao.observeGoals().map { it?.toDomain() }
    }

    override suspend fun getGoals(): GoalSettings? = dao.getGoals()?.toDomain()

    override suspend fun saveGoals(goals: GoalSettings) {
        dao.upsert(goals.toEntity())
    }
}

private fun GoalEntity.toDomain(): GoalSettings {
    return GoalSettings(
        daysOfWeek = daysOfWeek.filter { it in 1..7 }.toSet(),
        timesOfDay = timesOfDay
            .mapNotNull { raw ->
                runCatching { LocalTime.parse(raw) }.getOrNull()
            }
            .distinct()
            .sorted(),
        alarmNote = alarmNote.ifBlank { null },
    )
}

private fun GoalSettings.toEntity(): GoalEntity {
    return GoalEntity(
        daysOfWeek = daysOfWeek.filter { it in 1..7 }.sorted(),
        timesOfDay = timesOfDay.distinct().sorted().map { it.toString() },
        alarmNote = alarmNote.orEmpty().trim(),
    )
}
