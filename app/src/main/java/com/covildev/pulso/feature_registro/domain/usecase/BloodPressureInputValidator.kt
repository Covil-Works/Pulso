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
            return "Sistólica fora da faixa esperada ($MIN_SYSTOLIC-$MAX_SYSTOLIC mmHg)."
        }

        if (diastolic !in MIN_DIASTOLIC..MAX_DIASTOLIC) {
            return "Diastólica fora da faixa esperada ($MIN_DIASTOLIC-$MAX_DIASTOLIC mmHg)."
        }

        if (systolic <= diastolic) {
            return "A sistólica deve ser maior que a diastólica."
        }

        if ((systolic - diastolic) < MIN_PULSE_PRESSURE) {
            return "A diferença entre sistólica e diastólica parece inválida."
        }

        return null
    }
}
