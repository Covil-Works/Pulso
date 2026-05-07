package com.covildev.pulso.feature_registro.domain.usecase

data class BloodPressureValidationErrors(
    val systolicError: String? = null,
    val diastolicError: String? = null,
) {
    val hasErrors: Boolean
        get() = systolicError != null || diastolicError != null
}

object BloodPressureInputValidator {
    const val MIN_PRESSURE = 0
    const val MAX_PRESSURE = 300

    private const val LOW_SYSTOLIC_SUSPECT = 70
    private const val HIGH_SYSTOLIC_SUSPECT = 250
    private const val LOW_DIASTOLIC_SUSPECT = 40
    private const val HIGH_DIASTOLIC_SUSPECT = 150
    private const val MIN_PULSE_PRESSURE_SUSPECT = 10
    private const val MAX_PULSE_PRESSURE_SUSPECT = 140

    fun validate(
        systolic: Int,
        diastolic: Int,
    ): BloodPressureValidationErrors {
        val systolicError = if (systolic !in MIN_PRESSURE..MAX_PRESSURE) {
            "A pressão sistólica deve estar entre $MIN_PRESSURE e $MAX_PRESSURE."
        } else {
            null
        }

        val diastolicError = when {
            diastolic !in MIN_PRESSURE..MAX_PRESSURE ->
                "A pressão diastólica deve estar entre $MIN_PRESSURE e $MAX_PRESSURE."
            diastolic > systolic ->
                "A pressão diastólica não pode ser maior que a sistólica."
            else -> null
        }

        return BloodPressureValidationErrors(
            systolicError = systolicError,
            diastolicError = diastolicError,
        )
    }

    fun isUnusualButAllowed(
        systolic: Int,
        diastolic: Int,
    ): Boolean {
        if (validate(systolic, diastolic).hasErrors) return false

        val pulsePressure = systolic - diastolic
        return systolic < LOW_SYSTOLIC_SUSPECT ||
            systolic > HIGH_SYSTOLIC_SUSPECT ||
            diastolic < LOW_DIASTOLIC_SUSPECT ||
            diastolic > HIGH_DIASTOLIC_SUSPECT ||
            pulsePressure < MIN_PULSE_PRESSURE_SUSPECT ||
            pulsePressure > MAX_PULSE_PRESSURE_SUSPECT
    }
}
