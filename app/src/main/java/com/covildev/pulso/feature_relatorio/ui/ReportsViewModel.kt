package com.covildev.pulso.feature_relatorio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_relatorio.domain.model.GeneratedReport
import com.covildev.pulso.feature_relatorio.domain.usecase.GeneratePdfReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val isGenerating: Boolean = false,
    val generatedReport: GeneratedReport? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val generatePdfReportUseCase: GeneratePdfReportUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun generateReport() {
        if (_uiState.value.isGenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            val result = generatePdfReportUseCase()
            _uiState.update { current ->
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
