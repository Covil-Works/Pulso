package com.covildev.pulso.feature_registro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProfileRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    var systolicInput by rememberSaveable { mutableStateOf("") }
    var diastolicInput by rememberSaveable { mutableStateOf("") }
    var includeNotes by rememberSaveable { mutableStateOf(false) }
    var notesInput by rememberSaveable { mutableStateOf("") }

    fun resetBottomSheet() {
        systolicInput = ""
        diastolicInput = ""
        includeNotes = false
        notesInput = ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Principal") },
                actions = {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Perfil") },
                            onClick = {
                                showOptionsMenu = false
                                onProfileRequested()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar registro")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                MetricsCard(uiState = uiState)
            }
            item {
                Text(
                    text = "Historico do mes",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            if (uiState.monthlyRecords.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum registro neste mes ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = uiState.monthlyRecords,
                    key = { it.id },
                ) { record ->
                    RecordCard(record = record)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Novo registro",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = systolicInput,
                        onValueChange = { systolicInput = it.filter(Char::isDigit) },
                        label = { Text("Sistolica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = diastolicInput,
                        onValueChange = { diastolicInput = it.filter(Char::isDigit) },
                        label = { Text("Diastolica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeNotes,
                        onCheckedChange = { includeNotes = it },
                    )
                    Text("Adicionar observacao")
                }
                if (includeNotes) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Observacao") },
                    )
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        coroutineScope.launch {
                            val saveResult = viewModel.addRecord(
                                systolicInput = systolicInput,
                                diastolicInput = diastolicInput,
                                notes = notesInput.takeIf { includeNotes },
                            )
                            if (saveResult.isSuccess) {
                                showBottomSheet = false
                                resetBottomSheet()
                                val riskLabel = saveResult.getOrNull()?.label ?: ""
                                snackbarHostState.showSnackbar("Registro salvo ($riskLabel).")
                            } else {
                                snackbarHostState.showSnackbar(
                                    saveResult.exceptionOrNull()?.message
                                        ?: "Nao foi possivel salvar o registro.",
                                )
                            }
                        }
                    },
                ) {
                    Text("Salvar")
                }
            }
        }
    }
}

@Composable
private fun MetricsCard(uiState: DashboardUiState) {
    Card(
        colors = CardDefaults.cardColors(
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
                text = "Metricas resumidas",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Pressao media: ${uiState.averageSystolic ?: "-"} / ${uiState.averageDiastolic ?: "-"} mmHg",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Sequencia atual: ${uiState.streakDays} dia(s) seguido(s).",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun RecordCard(record: BloodPressureRecord) {
    val riskColor = when (record.riskLevel) {
        RiskLevel.GOOD -> Color(0xFF2E7D32)
        RiskLevel.WARNING -> Color(0xFFF9A825)
        RiskLevel.RISK -> Color(0xFFC62828)
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")) }
    val dateText = dateFormatter.format(
        Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()),
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = riskColor.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$dateText - ${record.systolic}/${record.diastolic} mmHg",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Classificacao: ${record.riskLevel.label}",
                color = riskColor,
            )
            record.notes?.let { note ->
                Text(
                    text = "Obs: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}



