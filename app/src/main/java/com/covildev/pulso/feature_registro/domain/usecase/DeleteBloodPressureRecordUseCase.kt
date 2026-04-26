package com.covildev.pulso.feature_registro.domain.usecase

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import javax.inject.Inject

class DeleteBloodPressureRecordUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    suspend operator fun invoke(record: BloodPressureRecord): Result<Unit> {
        if (record.id <= 0L) {
            return Result.failure(
                IllegalArgumentException("Registro invalido para exclusao."),
            )
        }
        repository.deleteRecord(record.id)
        return Result.success(Unit)
    }
}
