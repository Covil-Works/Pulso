package com.covildev.pulso.feature_metas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_settings")
data class GoalEntity(
    @PrimaryKey
    val id: Int = 1,
    val daysOfWeek: List<Int>,
    val timesOfDay: List<String>,
    val alarmNote: String = "",
)
