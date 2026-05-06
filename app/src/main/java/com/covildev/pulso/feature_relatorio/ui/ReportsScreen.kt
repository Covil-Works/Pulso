package com.covildev.pulso.feature_relatorio.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.model.RiskLevel
import com.covildev.pulso.feature_registro.domain.usecase.BloodPressureInputValidator
import com.covildev.pulso.feature_registro.domain.usecase.BloodPressureValidationErrors
import com.covildev.pulso.ui.theme.LightSectionBackground
import com.covildev.pulso.ui.theme.PureWhite
import com.covildev.pulso.ui.theme.SecondaryBlueLight
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReportCardBorderColor = Color(0xFFE6EAF2)
private val InactiveRecordBorderColor = Color(0xFFE7EAF0)

private data class RiskBadgeStyle(
    val icon: ImageVector,
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color,
)

private data class ReportMetrics(
    val totalRecords: Int = 0,
    val lastRecordTimestamp: Long? = null,
    val goodCount: Int = 0,
    val warningCount: Int = 0,
    val riskCount: Int = 0,
)

private data class ParsedPressureForm(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val errors: BloodPressureValidationErrors = BloodPressureValidationErrors(),
) {
    val isValid: Boolean
        get() = systolic != null && diastolic != null && !errors.hasErrors
}

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
    var systolicError by rememberSaveable { mutableStateOf<String?>(null) }
    var diastolicError by rememberSaveable { mutableStateOf<String?>(null) }
    var notesInput by rememberSaveable { mutableStateOf("") }
    var pendingUnusualSave by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reportMetrics = remember(uiState.records) { buildReportMetrics(uiState.records) }
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
        systolicError = null
        diastolicError = null
        notesInput = record.notes.orEmpty()
        pendingUnusualSave = null
        editingRecord = record
    }

    fun parseAndValidatePressureInputs(): ParsedPressureForm {
        val systolic = systolicInput.toIntOrNull()
        val diastolic = diastolicInput.toIntOrNull()

        val requiredErrors = BloodPressureValidationErrors(
            systolicError = if (systolic == null) "Informe a pressao sistolica." else null,
            diastolicError = if (diastolic == null) "Informe a pressao diastolica." else null,
        )
        if (requiredErrors.hasErrors) {
            return ParsedPressureForm(
                systolic = systolic,
                diastolic = diastolic,
                errors = requiredErrors,
            )
        }

        return ParsedPressureForm(
            systolic = systolic,
            diastolic = diastolic,
            errors = BloodPressureInputValidator.validate(
                systolic = systolic!!,
                diastolic = diastolic!!,
            ),
        )
    }

    suspend fun persistRecordUpdate(
        record: BloodPressureRecord,
        systolic: Int,
        diastolic: Int,
    ) {
        val saveResult = viewModel.updateRecord(
            record = record,
            systolic = systolic,
            diastolic = diastolic,
            notes = notesInput,
        )
        if (saveResult.isSuccess) {
            editingRecord = null
            expandedRecordId = null
            pendingUnusualSave = null
            snackbarHostState.showSnackbar(
                "Registro atualizado (${saveResult.getOrNull()?.label.orEmpty()}).",
            )
        } else {
            snackbarHostState.showSnackbar(
                saveResult.exceptionOrNull()?.message
                    ?: "Nao foi possivel salvar o registro.",
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LightSectionBackground,
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
                .padding(innerPadding)
                .background(LightSectionBackground),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ReportsHeroSection(metrics = reportMetrics)
            }
            item {
                ReportGenerationSection(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    isGenerating = uiState.isGenerating,
                    onGenerateReport = viewModel::generateReport,
                )
            }
            item {
                ReportsSectionHeader(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    title = "Histórico completo",
                    icon = Icons.Outlined.CalendarToday,
                )
            }
            if (uiState.records.isEmpty()) {
                item {
                    ReportsEmptyState(
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
            } else {
                items(
                    items = uiState.records,
                    key = { it.id },
                ) { record ->
                    ReportRecordCard(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        record = record,
                        isExpanded = expandedRecordId == record.id,
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
            containerColor = PureWhite,
            onDismissRequest = {
                pendingUnusualSave = null
                editingRecord = null
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
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Text(
                            text = "Editar registro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = PureWhite,
                        ),
                        onClick = {
                            coroutineScope.launch {
                                val parsedForm = parseAndValidatePressureInputs()
                                systolicError = parsedForm.errors.systolicError
                                diastolicError = parsedForm.errors.diastolicError
                                if (!parsedForm.isValid) return@launch

                                val systolic = parsedForm.systolic!!
                                val diastolic = parsedForm.diastolic!!
                                if (BloodPressureInputValidator.isUnusualButAllowed(systolic, diastolic)) {
                                    pendingUnusualSave = systolic to diastolic
                                    return@launch
                                }

                                persistRecordUpdate(
                                    record = currentEditingRecord,
                                    systolic = systolic,
                                    diastolic = diastolic,
                                )
                                return@launch

                                val saveResult = viewModel.updateRecord(
                                    record = currentEditingRecord,
                                    systolic = systolicInput.toIntOrNull() ?: -1,
                                    diastolic = diastolicInput.toIntOrNull() ?: -1,
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = systolicInput,
                        onValueChange = {
                            systolicInput = it.filter(Char::isDigit)
                            systolicError = null
                            diastolicError = null
                        },
                        isError = systolicError != null,
                        label = { Text("Sistólica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            systolicError?.let {
                                Text(it)
                            }
                        },
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = diastolicInput,
                        onValueChange = {
                            diastolicInput = it.filter(Char::isDigit)
                            systolicError = null
                            diastolicError = null
                        },
                        isError = diastolicError != null,
                        label = { Text("Diastólica") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            diastolicError?.let {
                                Text(it)
                            }
                        },
                    )
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Observação") },
                )
            }
        }
    }

    val unusualSave = pendingUnusualSave
    if (currentEditingRecord != null && unusualSave != null) {
        AlertDialog(
            onDismissRequest = { pendingUnusualSave = null },
            title = { Text("Confirmar valores") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tem certeza que sua pressão é essa?")
                    Text(
                        text = "${unusualSave.first}/${unusualSave.second}mmHg",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = PureWhite,
                    ),
                    onClick = {
                        coroutineScope.launch {
                            persistRecordUpdate(
                                record = currentEditingRecord,
                                systolic = unusualSave.first,
                                diastolic = unusualSave.second,
                            )
                        }
                    },
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingUnusualSave = null
                    },
                ) {
                    Text("Corrigir")
                }
            },
        )
    }
}

@Composable
private fun ReportsHeroSection(metrics: ReportMetrics) {
    val lastRecordLabel = metrics.lastRecordTimestamp?.let(::formatShortDate) ?: "--"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ReportsSectionHeader(
                title = "Resumo dos Registros",
                icon = Icons.Outlined.CheckCircle,
            )

            if (metrics.totalRecords == 0) {
                Text(
                    text = "Quando você adicionar registros na tela Principal, o resumo aparece aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryBlueLight,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReportMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Registros",
                    value = metrics.totalRecords.toString(),
                )
                ReportMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Último registro",
                    value = lastRecordLabel,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RiskCountPill(
                    modifier = Modifier.weight(1f),
                    label = "Normal",
                    count = metrics.goodCount,
                    style = riskBadgeStyleFor(RiskLevel.GOOD),
                )
                RiskCountPill(
                    modifier = Modifier.weight(1f),
                    label = "Alerta",
                    count = metrics.warningCount,
                    style = riskBadgeStyleFor(RiskLevel.WARNING),
                )
                RiskCountPill(
                    modifier = Modifier.weight(1f),
                    label = "Elevada",
                    count = metrics.riskCount,
                    style = riskBadgeStyleFor(RiskLevel.RISK),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = InactiveRecordBorderColor,
        )
    }
}

@Composable
private fun ReportMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = LightSectionBackground,
        border = BorderStroke(1.dp, ReportCardBorderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryBlueLight,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RiskCountPill(
    label: String,
    count: Int,
    style: RiskBadgeStyle,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = style.containerColor,
        border = BorderStroke(1.dp, style.borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.contentColor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelMedium,
                color = style.contentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReportsSectionHeader(
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
private fun ReportGenerationSection(
    isGenerating: Boolean,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, ReportCardBorderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = "Relatório em PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Gere um resumo completo para compartilhar com médicos ou salvar no celular.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryBlueLight,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = onGenerateReport,
                enabled = !isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = PureWhite,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    disabledContentColor = PureWhite.copy(alpha = 0.7f),
                ),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PureWhite,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerando PDF...")
                } else {
                    Text("Gerar relatório")
                }
            }
        }
    }
}

@Composable
private fun ReportsEmptyState(modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, ReportCardBorderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Nenhum registro encontrado.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Adicione registros na tela Principal para visualizar o histórico aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryBlueLight,
            )
        }
    }
}

@Composable
private fun ReportRecordCard(
    record: BloodPressureRecord,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val riskBadgeStyle = remember(record.systolic, record.diastolic) {
        riskBadgeStyleFor(RiskLevel.fromPressure(record.systolic, record.diastolic))
    }
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded) Color(0xFFF6F9FF) else PureWhite,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reportCardContainerColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.secondary else InactiveRecordBorderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reportCardBorderColor",
    )
    val borderWidth: Dp = if (isExpanded) 1.6.dp else 1.dp
    val dateText = remember(record.timestamp) { formatRecordDate(record.timestamp) }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = borderWidth, color = borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = SecondaryBlueLight,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryBlueLight,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = record.systolic.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text = "/${record.diastolic}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SecondaryBlueLight,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "mmHg",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryBlueLight,
                            modifier = Modifier
                                .alignByBaseline()
                                .padding(bottom = 2.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = riskBadgeStyle.containerColor,
                    border = BorderStroke(1.dp, riskBadgeStyle.borderColor),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = riskBadgeStyle.icon,
                            contentDescription = null,
                            tint = riskBadgeStyle.contentColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = RiskLevel.fromPressure(record.systolic, record.diastolic).label,
                            style = MaterialTheme.typography.labelMedium,
                            color = riskBadgeStyle.contentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    record.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = "Obs: $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onEditClick,
                            colors = ButtonDefaults.textButtonColors(
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
                            colors = ButtonDefaults.textButtonColors(
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

private fun buildReportMetrics(records: List<BloodPressureRecord>): ReportMetrics {
    if (records.isEmpty()) return ReportMetrics()

    var goodCount = 0
    var warningCount = 0
    var riskCount = 0

    records.forEach { record ->
        when (RiskLevel.fromPressure(record.systolic, record.diastolic)) {
            RiskLevel.GOOD -> goodCount += 1
            RiskLevel.WARNING -> warningCount += 1
            RiskLevel.RISK -> riskCount += 1
        }
    }

    val totalRecords = records.size
    return ReportMetrics(
        totalRecords = totalRecords,
        lastRecordTimestamp = records.maxOfOrNull { it.timestamp },
        goodCount = goodCount,
        warningCount = warningCount,
        riskCount = riskCount,
    )
}

private fun riskBadgeStyleFor(riskLevel: RiskLevel): RiskBadgeStyle {
    return when (riskLevel) {
        RiskLevel.GOOD -> RiskBadgeStyle(
            icon = Icons.Outlined.CheckCircle,
            containerColor = Color(0xFFEAF8EF),
            borderColor = Color(0xFFC7E8D3),
            contentColor = Color(0xFF1F7A46),
        )
        RiskLevel.WARNING -> RiskBadgeStyle(
            icon = Icons.Outlined.ErrorOutline,
            containerColor = Color(0xFFFFF6E8),
            borderColor = Color(0xFFF8DFB3),
            contentColor = Color(0xFFB26A00),
        )
        RiskLevel.RISK -> RiskBadgeStyle(
            icon = Icons.Outlined.ErrorOutline,
            containerColor = Color(0xFFFFEEEF),
            borderColor = Color(0xFFF8C9CE),
            contentColor = Color(0xFFB3262E),
        )
    }
}

private fun formatRecordDate(timestamp: Long): String {
    val locale = Locale.forLanguageTag("pt-BR")
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime()
    val date = dateTime.toLocalDate()
    val today = LocalDate.now(zone)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", locale)

    return when (date) {
        today -> "Hoje, ${dateTime.format(timeFormatter)}"
        today.minusDays(1) -> "Ontem, ${dateTime.format(timeFormatter)}"
        else -> dateTime.format(fullFormatter)
    }
}

private fun formatShortDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    return date.format(formatter)
}
