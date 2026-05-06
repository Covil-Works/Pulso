package com.covildev.pulso.feature_registro.domain.usecase

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import javax.inject.Inject

class AddBloodPressureRecordUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    suspend operator fun invoke(
        systolic: Int,
        diastolic: Int,
        notes: String?,
    ): Result<RiskLevel> {
        val validationError = BloodPressureInputValidator.validate(
            systolic = systolic,
            diastolic = diastolic,
        )
        if (validationError.hasErrors) {
            return Result.failure(
                IllegalArgumentException(
                    validationError.systolicError
                        ?: validationError.diastolicError
                        ?: "Nao foi possivel validar os campos de pressao.",
                ),
            )
        }

        val riskLevel = RiskLevel.fromPressure(
            systolic = systolic,
            diastolic = diastolic,
        )
        val normalizedNote = notes?.trim().orEmpty().ifBlank { null }
        repository.insertRecord(
            BloodPressureRecord(
                systolic = systolic,
                diastolic = diastolic,
                timestamp = System.currentTimeMillis(),
                notes = normalizedNote,
                riskLevel = riskLevel,
            ),
        )
        return Result.success(riskLevel)
    }
}
