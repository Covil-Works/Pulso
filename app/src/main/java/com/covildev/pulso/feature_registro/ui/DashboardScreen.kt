package com.covildev.pulso.feature_registro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.ui.theme.RiskGood
import com.covildev.pulso.ui.theme.RiskHigh
import com.covildev.pulso.ui.theme.RiskWarning
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_RECENT_RECORDS = 3

private sealed interface DashboardBottomSheetMode {
    data object NewRecord : DashboardBottomSheetMode
    data class EditRecord(val record: BloodPressureRecord) : DashboardBottomSheetMode
}

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
    var expandedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var bottomSheetMode by remember { mutableStateOf<DashboardBottomSheetMode?>(null) }

    var systolicInput by rememberSaveable { mutableStateOf("") }
    var diastolicInput by rememberSaveable { mutableStateOf("") }
    var includeNotes by rememberSaveable { mutableStateOf(false) }
    var notesInput by rememberSaveable { mutableStateOf("") }
    val latestRecords = remember(uiState.records) { uiState.records.take(MAX_RECENT_RECORDS) }

    fun resetBottomSheetInputs() {
        systolicInput = ""
        diastolicInput = ""
        includeNotes = false
        notesInput = ""
    }

    fun openNewRecordSheet() {
        resetBottomSheetInputs()
        bottomSheetMode = DashboardBottomSheetMode.NewRecord
    }

    fun openEditRecordSheet(record: BloodPressureRecord) {
        systolicInput = record.systolic.toString()
        diastolicInput = record.diastolic.toString()
        notesInput = record.notes.orEmpty()
        includeNotes = record.notes != null
        bottomSheetMode = DashboardBottomSheetMode.EditRecord(record)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Principal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
            FloatingActionButton(
                onClick = { openNewRecordSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
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
                AveragePressureHeadline(
                    averageSystolic = uiState.averageSystolic,
                    averageDiastolic = uiState.averageDiastolic,
                )
            }
            item {
                CurrentStreakSection(streakDays = uiState.streakDays)
            }
            item {
                Text(
                    text = "Últimos",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            if (latestRecords.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum registro ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = latestRecords,
                    key = { it.id },
                ) { record ->
                    RecentRecordCard(
                        record = record,
                        isExpanded = expandedRecordId == record.id,
                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        onCardClick = {
                            expandedRecordId = if (expandedRecordId == record.id) null else record.id
                        },
                        onEditClick = { openEditRecordSheet(record) },
                    )
                }
            }
        }
    }

    val sheetMode = bottomSheetMode
    if (sheetMode != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (sheetMode is DashboardBottomSheetMode.EditRecord) {
                    expandedRecordId = null
                }
                bottomSheetMode = null
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (sheetMode is DashboardBottomSheetMode.EditRecord) {
                        "Editar registro"
                    } else {
                        "Novo registro"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = systolicInput,
                        onValueChange = { systolicInput = it.filter(Char::isDigit) },
                        label = { Text("Sistólica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = diastolicInput,
                        onValueChange = { diastolicInput = it.filter(Char::isDigit) },
                        label = { Text("Diastólica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                if (sheetMode is DashboardBottomSheetMode.NewRecord) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = includeNotes,
                            onCheckedChange = { includeNotes = it },
                        )
                        Text("Adicionar observação")
                    }
                }
                if (sheetMode is DashboardBottomSheetMode.EditRecord || includeNotes) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Observação") },
                    )
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    colors = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    onClick = {
                        coroutineScope.launch {
                            val saveResult = when (sheetMode) {
                                is DashboardBottomSheetMode.NewRecord -> {
                                    viewModel.addRecord(
                                        systolicInput = systolicInput,
                                        diastolicInput = diastolicInput,
                                        notes = notesInput.takeIf { includeNotes },
                                    )
                                }
                                is DashboardBottomSheetMode.EditRecord -> {
                                    viewModel.updateRecord(
                                        record = sheetMode.record,
                                        systolicInput = systolicInput,
                                        diastolicInput = diastolicInput,
                                        notes = notesInput,
                                    )
                                }
                            }
                            if (saveResult.isSuccess) {
                                bottomSheetMode = null
                                resetBottomSheetInputs()
                                val riskLabel = saveResult.getOrNull()?.label ?: ""
                                if (sheetMode is DashboardBottomSheetMode.EditRecord) {
                                    expandedRecordId = null
                                    snackbarHostState.showSnackbar("Registro atualizado ($riskLabel).")
                                } else {
                                    snackbarHostState.showSnackbar("Registro salvo ($riskLabel).")
                                }
                            } else {
                                snackbarHostState.showSnackbar(
                                    saveResult.exceptionOrNull()?.message
                                        ?: "Não foi possível salvar o registro.",
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
private fun AveragePressureHeadline(
    averageSystolic: Int?,
    averageDiastolic: Int?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Pressão média",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = averageSystolic?.toString() ?: "--",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "/",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = averageDiastolic?.toString() ?: "--",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "mmHg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun CurrentStreakSection(streakDays: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sequência atual",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = streakDays.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "dia(s) seguido(s)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentRecordCard(
    record: BloodPressureRecord,
    isExpanded: Boolean,
    activeContainerColor: Color,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val riskColor = when (record.riskLevel) {
        RiskLevel.GOOD -> RiskGood
        RiskLevel.WARNING -> RiskWarning
        RiskLevel.RISK -> RiskHigh
    }
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded) activeContainerColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "recordCardContainerColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "recordCardBorderColor",
    )
    val borderWidth: Dp = if (isExpanded) 1.5.dp else 1.dp
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")) }
    val dateText = dateFormatter.format(
        Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()),
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        onClick = onCardClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = borderWidth, color = borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$dateText - ${record.systolic}/${record.diastolic} mmHg",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Classificação: ${record.riskLevel.label}",
                color = riskColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            record.notes?.let { note ->
                Text(
                    text = "Obs: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    Spacer(modifier = Modifier.height(2.dp))
                    TextButton(
                        onClick = onEditClick,
                        colors = TextButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }
                }
            }
        }
    }
}



