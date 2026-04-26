package com.covildev.pulso.feature_metas.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.ui.MonthCalendar
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Metas") })
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
                MonthCalendar(
                    month = uiState.selectedMonth,
                    highlightedDays = uiState.highlightedDays,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    canGoNextMonth = uiState.selectedMonth < YearMonth.now(),
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Dias da semana",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DAY_OPTIONS.chunked(4).forEach { rowDays ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowDays.forEach { day ->
                                        FilterChip(
                                            selected = day.value in uiState.selectedDays,
                                            onClick = { viewModel.toggleDay(day.value) },
                                            label = { Text(day.label) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Horarios diarios",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (uiState.selectedTimes.isEmpty()) {
                            Text(
                                text = "Nenhum horario definido.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.selectedTimes.chunked(3).forEach { rowTimes ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowTimes.forEach { time ->
                                            AssistChip(
                                                onClick = { viewModel.removeTime(time) },
                                                label = { Text(timeFormatter.format(time)) },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remover horario",
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        TextButton(
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
                            Text(" Adicionar horario")
                        }
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val result = viewModel.saveGoals()
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar(
                                            "Metas salvas. Lembretes locais atualizados.",
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            result.exceptionOrNull()?.message
                                                ?: "Nao foi possivel salvar as metas.",
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

            item {
                Card(
                    colors = CardDefaults.cardColors(
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
}

private data class DayOption(val value: Int, val label: String)

private val DAY_OPTIONS = listOf(
    DayOption(1, "Seg"),
    DayOption(2, "Ter"),
    DayOption(3, "Qua"),
    DayOption(4, "Qui"),
    DayOption(5, "Sex"),
    DayOption(6, "Sab"),
    DayOption(7, "Dom"),
)
