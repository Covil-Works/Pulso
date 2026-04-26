package com.covildev.pulso.feature_metas.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.ui.MonthCalendar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR")) }
    var showAlarmEditor by rememberSaveable { mutableStateOf(false) }
    var selectedDayForRecords by rememberSaveable { mutableIntStateOf(-1) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Metas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Definir alarmes",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                OutlinedCard(
                    onClick = {
                        viewModel.startEditingGoals()
                        showAlarmEditor = true
                    },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Dias: ${formatSelectedDays(uiState.previewSelectedDays)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Horários: ${formatSelectedTimes(uiState.previewSelectedTimes, timeFormatter)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Registro do mês",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                MonthCalendar(
                    month = uiState.selectedMonth,
                    highlightedDays = uiState.highlightedDays,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    canGoNextMonth = uiState.selectedMonth < YearMonth.now(),
                    onDayClick = { day ->
                        if (!uiState.recordsByDay[day].isNullOrEmpty()) {
                            selectedDayForRecords = day
                        }
                    },
                )
            }
            item {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Progresso de metas",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = uiState.progressMessage,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }

    if (showAlarmEditor) {
        ModalBottomSheet(
            onDismissRequest = {
                showAlarmEditor = false
                viewModel.restoreEditorFromSaved()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Definir alarmes",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Dias da semana",
                    style = MaterialTheme.typography.titleSmall,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_OPTIONS.chunked(4).forEach { rowDays ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowDays.forEach { day ->
                                FilterChip(
                                    selected = day.value in uiState.editorSelectedDays,
                                    onClick = { viewModel.toggleDay(day.value) },
                                    label = { Text(day.shortLabel) },
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (uiState.editorSelectedTimes.isEmpty()) {
                    Text(
                        text = "Nenhum horário definido.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.editorSelectedTimes.chunked(3).forEach { rowTimes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowTimes.forEach { time ->
                                    AssistChip(
                                        onClick = { viewModel.removeTime(time) },
                                        label = { Text(timeFormatter.format(time)) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover horário",
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    colors = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    onClick = {
                        val now = LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.addTime(LocalTime.of(hour, minute))
                            },
                            now.hour,
                            now.minute,
                            true,
                        ).show()
                    },
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null)
                    Text(" Adicionar horário")
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    colors = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.saveGoals()
                            if (result.isSuccess) {
                                showAlarmEditor = false
                                snackbarHostState.showSnackbar(
                                    "Metas salvas. Lembretes locais atualizados.",
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    result.exceptionOrNull()?.message
                                        ?: "Não foi possível salvar as metas.",
                                )
                            }
                        }
                    },
                ) {
                    Text("Salvar metas")
                }
            }
        }
    }

    if (selectedDayForRecords != -1) {
        val records = uiState.recordsByDay[selectedDayForRecords].orEmpty()
        AlertDialog(
            onDismissRequest = { selectedDayForRecords = -1 },
            title = { Text("Registros no dia") },
            text = {
                DayRecordsContent(records = records)
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedDayForRecords = -1 },
                    colors = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("Fechar")
                }
            },
        )
    }
}

@Composable
private fun DayRecordsContent(records: List<BloodPressureRecord>) {
    val hourFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        if (records.isEmpty()) {
            Text(
                text = "Não há registros neste dia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        records.forEach { record ->
            OutlinedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val hour = hourFormatter.format(
                        Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()),
                    )
                    Text(
                        text = "${record.systolic}/${record.diastolic} mmHg",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Horário: $hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    record.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = "Observação: $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private data class DayOption(
    val value: Int,
    val shortLabel: String,
    val fullLabel: String,
)

private val DAY_OPTIONS = listOf(
    DayOption(1, "Seg", "Segunda"),
    DayOption(2, "Ter", "Terça"),
    DayOption(3, "Qua", "Quarta"),
    DayOption(4, "Qui", "Quinta"),
    DayOption(5, "Sex", "Sexta"),
    DayOption(6, "Sab", "Sábado"),
    DayOption(7, "Dom", "Domingo"),
)

private fun formatSelectedDays(days: Set<Int>): String {
    if (days.isEmpty()) return "Nenhum dia selecionado"
    val sortedDays = days.sorted()
    if (sortedDays == listOf(1, 2, 3, 4, 5, 6, 7)) {
        return "Segunda a Domingo"
    }
    val labels = sortedDays.mapNotNull { day ->
        DAY_OPTIONS.firstOrNull { option -> option.value == day }?.fullLabel
    }
    return when (labels.size) {
        0 -> "Nenhum dia selecionado"
        1 -> labels.first()
        2 -> "${labels[0]} e ${labels[1]}"
        else -> labels.dropLast(1).joinToString(", ") + " e ${labels.last()}"
    }
}

private fun formatSelectedTimes(
    times: List<LocalTime>,
    formatter: DateTimeFormatter,
): String {
    if (times.isEmpty()) return "Nenhum horário definido"
    return times.joinToString(", ") { formatter.format(it) }
}
