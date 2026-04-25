package com.covildev.pulso.feature_registro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.covildev.pulso.feature_registro.domain.model.RiskLevel

@Entity(tableName = "blood_pressure")
data class BloodPressureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long,
    val notes: String?,
    val riskLevel: RiskLevel,
)
