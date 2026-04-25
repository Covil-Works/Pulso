package com.covildev.pulso.feature_metas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_metas.domain.model.GoalSettings
import com.covildev.pulso.feature_metas.domain.usecase.ObserveGoalsUseCase
import com.covildev.pulso.feature_metas.domain.usecase.SaveGoalsUseCase
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.usecase.ObserveAllRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class GoalsUiState(
    val selectedDays: Set<Int> = emptySet(),
    val selectedTimes: List<LocalTime> = emptyList(),
    val progressMessage: String = "Defina os dias e horarios para montar sua rotina.",
)

private data class GoalEditorState(
    val selectedDays: Set<Int> = emptySet(),
    val selectedTimes: List<LocalTime> = emptyList(),
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    observeGoalsUseCase: ObserveGoalsUseCase,
    observeAllRecordsUseCase: ObserveAllRecordsUseCase,
    private val saveGoalsUseCase: SaveGoalsUseCase,
) : ViewModel() {
    private val editorState = MutableStateFlow(GoalEditorState())
    private val recordsFlow = observeAllRecordsUseCase().catch { emit(emptyList()) }

    val uiState: StateFlow<GoalsUiState> = combine(
        editorState,
        recordsFlow,
    ) { editor, records ->
        GoalsUiState(
            selectedDays = editor.selectedDays,
            selectedTimes = editor.selectedTimes,
            progressMessage = buildProgressMessage(
                selectedDays = editor.selectedDays,
                records = records,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState(),
    )

    init {
        viewModelScope.launch {
            observeGoalsUseCase()
                .catch { emit(null) }
                .collect { goals ->
                    if (goals != null) {
                        editorState.value = GoalEditorState(
                            selectedDays = goals.daysOfWeek,
                            selectedTimes = goals.timesOfDay.sorted(),
                        )
                    }
                }
        }
    }

    fun toggleDay(dayOfWeek: Int) {
        editorState.update { current ->
            val updatedDays = if (dayOfWeek in current.selectedDays) {
                current.selectedDays - dayOfWeek
            } else {
                current.selectedDays + dayOfWeek
            }
            current.copy(selectedDays = updatedDays)
        }
    }

    fun addTime(localTime: LocalTime) {
        editorState.update { current ->
            val updatedTimes = (current.selectedTimes + localTime).distinct().sorted()
            current.copy(selectedTimes = updatedTimes)
        }
    }

    fun removeTime(localTime: LocalTime) {
        editorState.update { current ->
            current.copy(selectedTimes = current.selectedTimes - localTime)
        }
    }

    suspend fun saveGoals(): Result<Unit> {
        val current = editorState.value
        return saveGoalsUseCase(
            GoalSettings(
                daysOfWeek = current.selectedDays,
                timesOfDay = current.selectedTimes,
            ),
        )
    }
}

private fun buildProgressMessage(
    selectedDays: Set<Int>,
    records: List<BloodPressureRecord>,
): String {
    if (selectedDays.isEmpty()) {
        return "Selecione dias da semana e horarios para ativar metas de lembrete."
    }

    val recordDates = records.map {
        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()

    val today = LocalDate.now()
    var expectedDays = 0
    var completedDays = 0
    for (offset in 0..6) {
        val date = today.minusDays(offset.toLong())
        if (date.dayOfWeek.value in selectedDays) {
            expectedDays += 1
            if (date in recordDates) {
                completedDays += 1
            }
        }
    }

    if (expectedDays == 0) {
        return "Nenhuma medicao esperada nos ultimos 7 dias com as regras atuais."
    }
    if (completedDays == expectedDays) {
        return "Excelente! Voce cumpriu $completedDays de $expectedDays dias previstos."
    }
    val missedDays = expectedDays - completedDays
    return "Voce registrou em $completedDays de $expectedDays dias previstos. Faltaram $missedDays dia(s)."
}
