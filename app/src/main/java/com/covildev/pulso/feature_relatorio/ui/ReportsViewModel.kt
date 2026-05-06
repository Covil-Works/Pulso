package com.covildev.pulso.feature_relatorio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.usecase.DeleteBloodPressureRecordUseCase
import com.covildev.pulso.feature_registro.domain.usecase.ObserveAllRecordsUseCase
import com.covildev.pulso.feature_registro.domain.usecase.UpdateBloodPressureRecordUseCase
import com.covildev.pulso.feature_relatorio.domain.model.GeneratedReport
import com.covildev.pulso.feature_relatorio.domain.usecase.GeneratePdfReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val records: List<BloodPressureRecord> = emptyList(),
    val isGenerating: Boolean = false,
    val generatedReport: GeneratedReport? = null,
    val errorMessage: String? = null,
)

private data class ReportGenerationState(
    val isGenerating: Boolean = false,
    val generatedReport: GeneratedReport? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val generatePdfReportUseCase: GeneratePdfReportUseCase,
    observeAllRecordsUseCase: ObserveAllRecordsUseCase,
    private val updateBloodPressureRecordUseCase: UpdateBloodPressureRecordUseCase,
    private val deleteBloodPressureRecordUseCase: DeleteBloodPressureRecordUseCase,
) : ViewModel() {
    private val reportGenerationState = MutableStateFlow(ReportGenerationState())
    private val recordsFlow = observeAllRecordsUseCase().catch { emit(emptyList()) }

    val uiState: StateFlow<ReportsUiState> = combine(
        recordsFlow,
        reportGenerationState,
    ) { records, reportState ->
        ReportsUiState(
            records = records,
            isGenerating = reportState.isGenerating,
            generatedReport = reportState.generatedReport,
            errorMessage = reportState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState(),
    )

    fun generateReport() {
        if (reportGenerationState.value.isGenerating) return
        viewModelScope.launch {
            reportGenerationState.update {
                it.copy(
                    isGenerating = true,
                    generatedReport = null,
                    errorMessage = null,
                )
            }
            val result = generatePdfReportUseCase()
            reportGenerationState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        isGenerating = false,
                        generatedReport = result.getOrNull(),
                        errorMessage = null,
                    )
                } else {
                    current.copy(
                        isGenerating = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Erro ao gerar PDF.",
                    )
                }
            }
        }
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

    suspend fun deleteRecord(record: BloodPressureRecord): Result<Unit> {
        return deleteBloodPressureRecordUseCase(record)
    }

    fun clearError() {
        reportGenerationState.update { it.copy(errorMessage = null) }
    }

    fun clearGeneratedReport() {
        reportGenerationState.update { it.copy(generatedReport = null) }
    }
}
