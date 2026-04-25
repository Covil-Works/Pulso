package com.covildev.pulso.feature_registro.domain.model

data class BloodPressureRecord(
    val id: Long = 0L,
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long,
    val notes: String?,
    val riskLevel: RiskLevel,
)
