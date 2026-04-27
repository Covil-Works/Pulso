package com.covildev.pulso.feature_registro.domain.usecase

object BloodPressureInputValidator {
    private const val MIN_SYSTOLIC = 60
    private const val MAX_SYSTOLIC = 280
    private const val MIN_DIASTOLIC = 30
    private const val MAX_DIASTOLIC = 180
    private const val MIN_PULSE_PRESSURE = 10

    fun validate(
        systolic: Int,
        diastolic: Int,
    ): String? {
        if (systolic !in MIN_SYSTOLIC..MAX_SYSTOLIC) {
            return "Sistolica fora da faixa esperada ($MIN_SYSTOLIC-$MAX_SYSTOLIC mmHg)."
        }

        if (diastolic !in MIN_DIASTOLIC..MAX_DIASTOLIC) {
            return "Diastolica fora da faixa esperada ($MIN_DIASTOLIC-$MAX_DIASTOLIC mmHg)."
        }

        if (systolic <= diastolic) {
            return "A sistolica deve ser maior que a diastolica."
        }

        if ((systolic - diastolic) < MIN_PULSE_PRESSURE) {
            return "A diferenca entre sistolica e diastolica parece invalida."
        }

        return null
    }
}
