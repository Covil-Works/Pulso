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
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class GoalsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val highlightedDays: Set<Int> = emptySet(),
    val recordsByDay: Map<Int, List<BloodPressureRecord>> = emptyMap(),
    val previewSelectedDays: Set<Int> = emptySet(),
    val previewSelectedTimes: List<LocalTime> = emptyList(),
    val editorSelectedDays: Set<Int> = emptySet(),
    val editorSelectedTimes: List<LocalTime> = emptyList(),
    val progressMessage: String = "Defina os dias e horários para montar sua rotina.",
    val progressFraction: Float = 0f,
    val progressLabel: String = "0%",
)

private data class GoalEditorState(
    val selectedDays: Set<Int> = emptySet(),
    val selectedTimes: List<LocalTime> = emptyList(),
)

private data class GoalProgress(
    val completedDays: Int,
    val expectedDays: Int,
    val message: String,
) {
    val fraction: Float
        get() = if (expectedDays == 0) 0f else completedDays.toFloat() / expectedDays.toFloat()

    val label: String
        get() = "${(fraction * 100).toInt()}%"
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    observeGoalsUseCase: ObserveGoalsUseCase,
    observeAllRecordsUseCase: ObserveAllRecordsUseCase,
    private val saveGoalsUseCase: SaveGoalsUseCase,
) : ViewModel() {
    private val editorState = MutableStateFlow(GoalEditorState())
    private val savedState = MutableStateFlow(GoalEditorState())
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val recordsFlow = observeAllRecordsUseCase().catch { emit(emptyList()) }

    val uiState: StateFlow<GoalsUiState> = combine(
        selectedMonth,
        savedState,
        editorState,
        recordsFlow,
    ) { month, saved, editor, records ->
        val progress = buildGoalProgress(
            selectedDays = saved.selectedDays,
            records = records,
        )
        val recordsByDay = records
            .mapNotNull { record ->
                val localDate = Instant.ofEpochMilli(record.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (YearMonth.from(localDate) == month) {
                    localDate.dayOfMonth to record
                } else {
                    null
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )

        GoalsUiState(
            selectedMonth = month,
            highlightedDays = recordsByDay.keys,
            recordsByDay = recordsByDay,
            previewSelectedDays = saved.selectedDays,
            previewSelectedTimes = saved.selectedTimes,
            editorSelectedDays = editor.selectedDays,
            editorSelectedTimes = editor.selectedTimes,
            progressMessage = progress.message,
            progressFraction = progress.fraction,
            progressLabel = progress.label,
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
                    val nextState = goals?.toEditorState() ?: GoalEditorState()
                    savedState.value = nextState
                    editorState.value = nextState
                }
        }
    }

    fun startEditingGoals() {
        editorState.value = savedState.value
    }

    fun restoreEditorFromSaved() {
        editorState.value = savedState.value
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

    fun goToPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        val currentMonth = YearMonth.now()
        if (selectedMonth.value < currentMonth) {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
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
        val result = saveGoalsUseCase(
            GoalSettings(
                daysOfWeek = current.selectedDays,
                timesOfDay = current.selectedTimes,
            ),
        )
        if (result.isSuccess) {
            savedState.value = current
        }
        return result
    }
}

private fun GoalSettings.toEditorState(): GoalEditorState {
    return GoalEditorState(
        selectedDays = daysOfWeek,
        selectedTimes = timesOfDay.sorted(),
    )
}

private fun buildGoalProgress(
    selectedDays: Set<Int>,
    records: List<BloodPressureRecord>,
): GoalProgress {
    if (selectedDays.isEmpty()) {
        return GoalProgress(
            completedDays = 0,
            expectedDays = 0,
            message = "Selecione dias da semana e horários para ativar metas de lembrete.",
        )
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
        return GoalProgress(
            completedDays = 0,
            expectedDays = 0,
            message = "Nenhuma medição esperada nos últimos 7 dias com as regras atuais.",
        )
    }
    if (completedDays == expectedDays) {
        return GoalProgress(
            completedDays = completedDays,
            expectedDays = expectedDays,
            message = "Excelente! Você cumpriu $completedDays de $expectedDays dias previstos.",
        )
    }
    val missedDays = expectedDays - completedDays
    return GoalProgress(
        completedDays = completedDays,
        expectedDays = expectedDays,
        message = "Você registrou em $completedDays de $expectedDays dias previstos. Faltaram $missedDays dia(s).",
    )
}
