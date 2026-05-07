package com.covildev.pulso.feature_registro.ui

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.covildev.pulso.ui.theme.SecondaryBlue
import com.covildev.pulso.ui.theme.SecondaryBlueLight
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_RECENT_RECORDS = 3
private val InactiveRecordBorderColor = Color(0xFFE7EAF0)

private sealed interface DashboardBottomSheetMode {
    data object NewRecord : DashboardBottomSheetMode
    data class EditRecord(val record: BloodPressureRecord) : DashboardBottomSheetMode
}

private data class RiskBadgeStyle(
    val icon: ImageVector,
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color,
)

private data class ParsedPressureForm(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val errors: BloodPressureValidationErrors = BloodPressureValidationErrors(),
) {
    val isValid: Boolean
        get() = systolic != null && diastolic != null && !errors.hasErrors
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onProfileRequested: () -> Unit,
    onViewAllRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showOptionsMenu by remember { mutableStateOf(false) }
    var expandedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var bottomSheetMode by remember { mutableStateOf<DashboardBottomSheetMode?>(null) }

    var systolicInput by rememberSaveable { mutableStateOf("") }
    var diastolicInput by rememberSaveable { mutableStateOf("") }
    var systolicError by rememberSaveable { mutableStateOf<String?>(null) }
    var diastolicError by rememberSaveable { mutableStateOf<String?>(null) }
    var includeNotes by rememberSaveable { mutableStateOf(false) }
    var notesInput by rememberSaveable { mutableStateOf("") }
    var pendingUnusualSave by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val latestRecords = remember(uiState.records) { uiState.records.take(MAX_RECENT_RECORDS) }

    fun resetBottomSheetInputs() {
        systolicInput = ""
        diastolicInput = ""
        systolicError = null
        diastolicError = null
        includeNotes = false
        notesInput = ""
        pendingUnusualSave = null
    }

    fun openNewRecordSheet() {
        resetBottomSheetInputs()
        bottomSheetMode = DashboardBottomSheetMode.NewRecord
    }

    fun openEditRecordSheet(record: BloodPressureRecord) {
        systolicInput = record.systolic.toString()
        diastolicInput = record.diastolic.toString()
        systolicError = null
        diastolicError = null
        notesInput = record.notes.orEmpty()
        includeNotes = record.notes != null
        pendingUnusualSave = null
        bottomSheetMode = DashboardBottomSheetMode.EditRecord(record)
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

    suspend fun persistRecord(
        sheetMode: DashboardBottomSheetMode,
        systolic: Int,
        diastolic: Int,
    ) {
        val saveResult = when (sheetMode) {
            is DashboardBottomSheetMode.NewRecord -> {
                viewModel.addRecord(
                    systolic = systolic,
                    diastolic = diastolic,
                    notes = notesInput.takeIf { includeNotes },
                )
            }
            is DashboardBottomSheetMode.EditRecord -> {
                viewModel.updateRecord(
                    record = sheetMode.record,
                    systolic = systolic,
                    diastolic = diastolic,
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
                    ?: "Nao foi possivel salvar o registro.",
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LightSectionBackground,
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
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 14.dp,
                    focusedElevation = 12.dp,
                    hoveredElevation = 12.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar registro",
                    modifier = Modifier.size(30.dp),
                )
            }
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
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardHeroSection(
                    averageSystolic = uiState.averageSystolic,
                    averageDiastolic = uiState.averageDiastolic,
                    streakDays = uiState.streakDays,
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Últimos registros",
                        style = MaterialTheme.typography.titleLarge,
                        color = SecondaryBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onViewAllRequested) {
                        Text(
                            text = "Ver todos",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryBlueLight,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (latestRecords.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 18.dp),
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
                        modifier = Modifier.padding(horizontal = 18.dp),
                        record = record,
                        isExpanded = expandedRecordId == record.id,
                        activeContainerColor = Color(0xFFF6F9FF),
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
        val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val notesBringIntoViewRequester = remember { BringIntoViewRequester() }
        ModalBottomSheet(
            sheetState = recordSheetState,
            containerColor = PureWhite,
            onDismissRequest = {
                if (sheetMode is DashboardBottomSheetMode.EditRecord) {
                    expandedRecordId = null
                }
                pendingUnusualSave = null
                bottomSheetMode = null
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (sheetMode is DashboardBottomSheetMode.EditRecord) {
                            "Editar registro"
                        } else {
                            "Novo registro"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White,
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

                                persistRecord(
                                    sheetMode = sheetMode,
                                    systolic = systolic,
                                    diastolic = diastolic,
                                )
                                return@launch

                                val saveResult = when (sheetMode) {
                                    is DashboardBottomSheetMode.NewRecord -> {
                                        viewModel.addRecord(
                                            systolic = systolicInput.toIntOrNull() ?: -1,
                                            diastolic = diastolicInput.toIntOrNull() ?: -1,
                                            notes = notesInput.takeIf { includeNotes },
                                        )
                                    }
                                    is DashboardBottomSheetMode.EditRecord -> {
                                        viewModel.updateRecord(
                                            record = sheetMode.record,
                                            systolic = systolicInput.toIntOrNull() ?: -1,
                                            diastolic = diastolicInput.toIntOrNull() ?: -1,
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
                if (sheetMode is DashboardBottomSheetMode.NewRecord) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = includeNotes,
                            onCheckedChange = { includeNotes = it },
                        )
                        Text("Adicionar observação")
                    }
                }
                if (sheetMode is DashboardBottomSheetMode.EditRecord || includeNotes) {
                    val dismissObservationKeyboard = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(notesBringIntoViewRequester)
                            .onFocusEvent { state ->
                                if (state.isFocused) {
                                    coroutineScope.launch {
                                        notesBringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        value = notesInput,
                        onValueChange = { updated ->
                            val isSubmitByNewline = updated.endsWith('\n')
                            if (isSubmitByNewline) {
                                notesInput = updated.removeSuffix("\n")
                                dismissObservationKeyboard()
                            } else {
                                notesInput = updated
                            }
                        },
                        label = { Text("Observação") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                dismissObservationKeyboard()
                                defaultKeyboardAction(ImeAction.Go)
                            },
                            onSearch = {
                                dismissObservationKeyboard()
                                defaultKeyboardAction(ImeAction.Search)
                            },
                            onSend = {
                                dismissObservationKeyboard()
                                defaultKeyboardAction(ImeAction.Send)
                            },
                            onNext = {
                                dismissObservationKeyboard()
                                defaultKeyboardAction(ImeAction.Next)
                            },
                            onDone = {
                                dismissObservationKeyboard()
                                defaultKeyboardAction(ImeAction.Done)
                            },
                        ),
                        maxLines = 4,
                    )
                }
            }
        }
    }

    val unusualSave = pendingUnusualSave
    if (sheetMode != null && unusualSave != null) {
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
                            persistRecord(
                                sheetMode = sheetMode,
                                systolic = unusualSave.first,
                                diastolic = unusualSave.second,
                            )
                            pendingUnusualSave = null
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
private fun DashboardHeroSection(
    averageSystolic: Int?,
    averageDiastolic: Int?,
    streakDays: Int,
) {
    val streakLabel = if (streakDays == 1) "1 dia" else "$streakDays dias seguidos"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Pressão Média (7 dias)",
                style = MaterialTheme.typography.labelLarge,
                color = SecondaryBlueLight,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = averageSystolic?.toString() ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryBlue,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "/",
                    style = MaterialTheme.typography.displayMedium,
                    color = SecondaryBlueLight,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = averageDiastolic?.toString() ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "mmHg",
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryBlueLight,
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(bottom = 8.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = LightSectionBackground,
                border = BorderStroke(1.dp, InactiveRecordBorderColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Sequência:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = streakLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
private fun RecentRecordCard(
    modifier: Modifier = Modifier,
    record: BloodPressureRecord,
    isExpanded: Boolean,
    activeContainerColor: Color,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val effectiveRiskLevel = remember(record.systolic, record.diastolic) {
        RiskLevel.fromPressure(record.systolic, record.diastolic)
    }
    val riskBadgeStyle = remember(effectiveRiskLevel) { riskBadgeStyleFor(effectiveRiskLevel) }
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded) activeContainerColor else PureWhite,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "recordCardContainerColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.secondary else InactiveRecordBorderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "recordCardBorderColor",
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
                            text = effectiveRiskLevel.label,
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
                    record.notes?.let { note ->
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
                    }
                }
            }
        }
    }
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

