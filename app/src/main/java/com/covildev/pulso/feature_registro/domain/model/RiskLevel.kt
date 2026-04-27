package com.covildev.pulso.feature_registro.domain.model

enum class RiskLevel(val label: String) {
    GOOD("Ótima"),
    WARNING("Atenção"),
    RISK("Risco");

    companion object {
        fun fromPressure(
            systolic: Int,
            diastolic: Int,
        ): RiskLevel {
            return when {
                systolic < 90 || diastolic < 60 -> RISK
                systolic >= 140 || diastolic >= 90 -> RISK
                systolic in 120..139 || diastolic in 80..89 -> WARNING
                systolic in 90..99 || diastolic in 60..69 -> WARNING
                else -> GOOD
            }
        }
    }
}
