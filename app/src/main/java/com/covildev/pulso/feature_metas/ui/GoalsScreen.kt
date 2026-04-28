package com.covildev.pulso.feature_metas.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.ui.MonthCalendar
import com.covildev.pulso.ui.theme.LightSectionBackground
import com.covildev.pulso.ui.theme.PureWhite
import com.covildev.pulso.ui.theme.SecondaryBlueLight
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
        containerColor = LightSectionBackground,
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
                .padding(innerPadding)
                .background(LightSectionBackground),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                GoalsSectionHeader(
                    title = "Definir alarmes",
                    icon = Icons.Outlined.Alarm,
                )
            }
            item {
                OutlinedCard(
                    onClick = {
                        viewModel.startEditingGoals()
                        showAlarmEditor = true
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = PureWhite,
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Lembretes configurados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Dias: ${formatSelectedDays(uiState.previewSelectedDays)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryBlueLight,
                        )
                        Text(
                            text = "Horarios: ${formatSelectedTimes(uiState.previewSelectedTimes, timeFormatter)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryBlueLight,
                        )
                        Surface(
                            color = Color(0xFFF1F4FA),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E7F2)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAlarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = "Toque para editar os alarmes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            item {
                GoalsSectionHeader(
                    title = "Registro do mes",
                    icon = Icons.Outlined.CalendarToday,
                )
            }
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = PureWhite,
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
                ) {
                    MonthCalendar(
                        modifier = Modifier.fillMaxWidth(),
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
            }
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = PureWhite,
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Progresso de metas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = uiState.progressLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { uiState.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = Color(0xFFE9EEF7),
                        )
                        Text(
                            text = uiState.progressMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryBlueLight,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF3FA),
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Default.AddAlarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Text(
                            text = "Definir alarmes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White,
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
                                            ?: "Nao foi possivel salvar as metas.",
                                    )
                                }
                            }
                        },
                    ) {
                        Text("Salvar")
                    }
                }
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
                    text = "Horarios",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (uiState.editorSelectedTimes.isEmpty()) {
                    Text(
                        text = "Nenhum horario definido.",
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
                    colors = ButtonDefaults.textButtonColors(
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
                    Text(" Adicionar horario")
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
                    colors = ButtonDefaults.textButtonColors(
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
private fun GoalsSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFEFF3FA),
        ) {
            Icon(
                modifier = Modifier.padding(8.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
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
                text = "Nao ha registros neste dia.",
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
                        text = "Horario: $hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    record.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = "Observacao: $note",
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
    DayOption(2, "Ter", "Terca"),
    DayOption(3, "Qua", "Quarta"),
    DayOption(4, "Qui", "Quinta"),
    DayOption(5, "Sex", "Sexta"),
    DayOption(6, "Sab", "Sabado"),
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
    if (times.isEmpty()) return "Nenhum horario definido"
    return times.joinToString(", ") { formatter.format(it) }
}
