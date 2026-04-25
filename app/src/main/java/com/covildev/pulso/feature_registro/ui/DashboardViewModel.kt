package com.covildev.pulso.feature_registro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.usecase.AddBloodPressureRecordUseCase
import com.covildev.pulso.feature_registro.domain.usecase.ObserveAllRecordsUseCase
import com.covildev.pulso.feature_registro.domain.usecase.ObserveRecordsForMonthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthlyRecords: List<BloodPressureRecord> = emptyList(),
    val highlightedDays: Set<Int> = emptySet(),
    val averageSystolic: Int? = null,
    val averageDiastolic: Int? = null,
    val streakDays: Int = 0,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val addBloodPressureRecordUseCase: AddBloodPressureRecordUseCase,
    observeAllRecordsUseCase: ObserveAllRecordsUseCase,
    observeRecordsForMonthUseCase: ObserveRecordsForMonthUseCase,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val allRecords = observeAllRecordsUseCase()
    private val monthlyRecords = selectedMonth.flatMapLatest { month ->
        observeRecordsForMonthUseCase(month)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        selectedMonth,
        monthlyRecords,
        allRecords,
    ) { month, monthRecords, records ->
        DashboardUiState(
            selectedMonth = month,
            monthlyRecords = monthRecords,
            highlightedDays = monthRecords.map {
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .dayOfMonth
            }.toSet(),
            averageSystolic = monthRecords.takeIf { it.isNotEmpty() }?.map { it.systolic }?.average()?.toInt(),
            averageDiastolic = monthRecords.takeIf { it.isNotEmpty() }?.map { it.diastolic }?.average()?.toInt(),
            streakDays = calculateStreak(records),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun goToPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        val currentMonth = YearMonth.now()
        if (selectedMonth.value < currentMonth) {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
        }
    }

    suspend fun addRecord(
        systolicInput: String,
        diastolicInput: String,
        notes: String?,
    ): Result<RiskLevel> {
        val systolic = systolicInput.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Informe a pressao sistolica."))
        val diastolic = diastolicInput.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Informe a pressao diastolica."))

        return addBloodPressureRecordUseCase(
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
