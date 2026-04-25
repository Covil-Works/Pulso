package com.covildev.pulso.feature_registro.domain.usecase

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class ObserveRecordsForMonthUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    operator fun invoke(month: YearMonth): Flow<List<BloodPressureRecord>> {
        val zoneId = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = month.atEndOfMonth()
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1
        return repository.observeRecordsInPeriod(startInclusive = start, endInclusive = end)
    }
}
