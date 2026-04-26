package com.covildev.pulso.feature_relatorio.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var expandedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingRecord by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var systolicInput by rememberSaveable { mutableStateOf("") }
    var diastolicInput by rememberSaveable { mutableStateOf("") }
    var notesInput by rememberSaveable { mutableStateOf("") }
    val shareReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.clearGeneratedReport()
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.generatedReport?.fileName) {
        val report = uiState.generatedReport ?: return@LaunchedEffect
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, report.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Pressão")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            shareReportLauncher.launch(Intent.createChooser(shareIntent, "Compartilhar ou salvar PDF"))
        }.onFailure {
            viewModel.clearGeneratedReport()
            snackbarHostState.showSnackbar("Não foi possível abrir o compartilhamento.")
        }
    }

    fun openEditRecordSheet(record: BloodPressureRecord) {
        systolicInput = record.systolic.toString()
        diastolicInput = record.diastolic.toString()
        notesInput = record.notes.orEmpty()
        editingRecord = record
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Relatórios") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
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
                ReportGenerationSection(
                    isGenerating = uiState.isGenerating,
                    onGenerateReport = viewModel::generateReport,
                )
            }
            if (uiState.isGenerating) {
                item {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            item {
                Text(
                    text = "Histórico completo",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            if (uiState.records.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum registro ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = uiState.records,
                    key = { it.id },
                ) { record ->
                    ReportRecordCard(
                        record = record,
                        isExpanded = expandedRecordId == record.id,
                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        onCardClick = {
                            expandedRecordId = if (expandedRecordId == record.id) null else record.id
                        },
                        onEditClick = { openEditRecordSheet(record) },
                        onDeleteClick = {
                            coroutineScope.launch {
                                val result = viewModel.deleteRecord(record)
                                if (result.isSuccess) {
                                    if (expandedRecordId == record.id) {
                                        expandedRecordId = null
                                    }
                                    snackbarHostState.showSnackbar("Registro excluído.")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.exceptionOrNull()?.message
                                            ?: "Não foi possível excluir o registro.",
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    val currentEditingRecord = editingRecord
    if (currentEditingRecord != null) {
        ModalBottomSheet(
            onDismissRequest = {
                editingRecord = null
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Editar registro",
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
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Observação") },
                )
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    colors = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    onClick = {
                        coroutineScope.launch {
                            val saveResult = viewModel.updateRecord(
                                record = currentEditingRecord,
                                systolicInput = systolicInput,
                                diastolicInput = diastolicInput,
                                notes = notesInput,
                            )
                            if (saveResult.isSuccess) {
                                editingRecord = null
                                expandedRecordId = null
                                snackbarHostState.showSnackbar(
                                    "Registro atualizado (${saveResult.getOrNull()?.label.orEmpty()}).",
                                )
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
private fun ReportGenerationSection(
    isGenerating: Boolean,
    onGenerateReport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Relatório completo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onGenerateReport,
                enabled = !isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
            ) {
                Text("Gerar relatório em PDF")
            }
        }
    }
}
@Composable
private fun ReportRecordCard(
    record: BloodPressureRecord,
    isExpanded: Boolean,
    activeContainerColor: Color,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val riskColor = when (record.riskLevel) {
        RiskLevel.GOOD -> RiskGood
        RiskLevel.WARNING -> RiskWarning
        RiskLevel.RISK -> RiskHigh
    }
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded) activeContainerColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reportCardContainerColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reportCardBorderColor",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        TextButton(
                            onClick = onDeleteClick,
                            colors = TextButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excluir")
                        }
                    }
                }
            }
        }
    }
}
