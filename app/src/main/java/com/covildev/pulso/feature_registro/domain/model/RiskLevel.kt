package com.covildev.pulso.feature_registro.domain.model

enum class RiskLevel(val label: String) {
    GOOD("Normal"),
    WARNING("Alerta"),
    RISK("Elevada");

    companion object {
        fun fromPressure(
            systolic: Int,
            diastolic: Int,
        ): RiskLevel {
            return when {
                systolic >= 140 || diastolic >= 90 -> RISK
                systolic in 120..139 || diastolic in 80..89 -> WARNING
                else -> GOOD
            }
        }
    }
}
