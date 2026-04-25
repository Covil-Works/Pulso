package com.covildev.pulso.feature_registro.domain.usecase

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAllRecordsUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    operator fun invoke(): Flow<List<BloodPressureRecord>> = repository.observeAllRecords()
}
