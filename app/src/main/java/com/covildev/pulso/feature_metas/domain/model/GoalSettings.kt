package com.covildev.pulso.feature_metas.domain.model

import java.time.LocalTime

data class GoalSettings(
    val daysOfWeek: Set<Int>,
    val timesOfDay: List<LocalTime>,
    val alarmNote: String? = null,
)
