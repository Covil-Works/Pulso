package com.covildev.pulso.feature_registro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.usecase.AddBloodPressureRecordUseCase
import com.covildev.pulso.feature_registro.domain.usecase.ObserveAllRecordsUseCase
import com.covildev.pulso.feature_registro.domain.usecase.UpdateBloodPressureRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val records: List<BloodPressureRecord> = emptyList(),
    val averageSystolic: Int? = null,
    val averageDiastolic: Int? = null,
    val streakDays: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val addBloodPressureRecordUseCase: AddBloodPressureRecordUseCase,
    private val updateBloodPressureRecordUseCase: UpdateBloodPressureRecordUseCase,
    observeAllRecordsUseCase: ObserveAllRecordsUseCase,
) : ViewModel() {
    private val allRecords = observeAllRecordsUseCase()

    val uiState: StateFlow<DashboardUiState> = allRecords.map { records ->
        val rollingWindowRecords = records.filterRecordsFromLastDays(days = 7)
        DashboardUiState(
            records = records,
            averageSystolic = rollingWindowRecords
                .takeIf { it.isNotEmpty() }
                ?.map { it.systolic }
                ?.average()
                ?.toInt(),
            averageDiastolic = rollingWindowRecords
                .takeIf { it.isNotEmpty() }
                ?.map { it.diastolic }
                ?.average()
                ?.toInt(),
            streakDays = calculateStreak(records),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    suspend fun addRecord(
        systolic: Int,
        diastolic: Int,
        notes: String?,
    ): Result<RiskLevel> {
        return addBloodPressureRecordUseCase(
            systolic = systolic,
            diastolic = diastolic,
            notes = notes,
        )
    }

    suspend fun updateRecord(
        record: BloodPressureRecord,
        systolic: Int,
        diastolic: Int,
        notes: String?,
    ): Result<RiskLevel> {
        return updateBloodPressureRecordUseCase(
            record = record,
            systolic = systolic,
            diastolic = diastolic,
            notes = notes,
        )
    }
}

private fun calculateStreak(records: List<BloodPressureRecord>): Int {
    val recordedDates = records.map {
        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()

    var streak = 0
    var cursor = LocalDate.now()
    while (recordedDates.contains(cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun List<BloodPressureRecord>.filterRecordsFromLastDays(
    days: Long,
): List<BloodPressureRecord> {
    val zone = ZoneId.systemDefault()
    val startDate = LocalDate.now(zone).minusDays(days - 1)
    val startInstant = startDate.atStartOfDay(zone).toInstant()
    return filter { record ->
        Instant.ofEpochMilli(record.timestamp).isAfter(startInstant.minusMillis(1))
    }
}
