package com.covildev.pulso.feature_registro.domain.usecase

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import javax.inject.Inject

class UpdateBloodPressureRecordUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    suspend operator fun invoke(
        record: BloodPressureRecord,
        systolic: Int,
        diastolic: Int,
        notes: String?,
    ): Result<RiskLevel> {
        val validationError = BloodPressureInputValidator.validate(
            systolic = systolic,
            diastolic = diastolic,
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val updatedRecord = record.copy(
            systolic = systolic,
            diastolic = diastolic,
            notes = notes?.trim().orEmpty().ifBlank { null },
            riskLevel = RiskLevel.fromPressure(systolic = systolic, diastolic = diastolic),
        )
        repository.updateRecord(updatedRecord)
        return Result.success(updatedRecord.riskLevel)
    }
}
