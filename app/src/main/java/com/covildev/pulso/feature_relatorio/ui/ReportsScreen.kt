package com.covildev.pulso.feature_relatorio.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_relatorio.domain.model.GeneratedReport
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Relatorios") })
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Gere um PDF com cabecalho do paciente, picos, media e historico detalhado.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = viewModel::generateReport,
                enabled = !uiState.isGenerating,
            ) {
                Text("Gerar relatorio em PDF")
            }
            if (uiState.isGenerating) {
                CircularProgressIndicator()
            }
            uiState.generatedReport?.let { report ->
                ReportSummaryCard(report = report)
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, report.uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Relatorio de Pressao")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatorio"))
                    },
                ) {
                    Text("Compartilhar PDF")
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: GeneratedReport) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
    }
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
            Text("Arquivo: ${report.fileName}", style = MaterialTheme.typography.titleSmall)
            val highest = report.summary.highestRecord
            val lowest = report.summary.lowestRecord
            Text("Pico maximo: ${formatRecord(highest, formatter)}")
            Text("Pico minimo: ${formatRecord(lowest, formatter)}")
            Text(
                "Pressao media: ${report.summary.averageSystolic ?: "-"} / " +
                    "${report.summary.averageDiastolic ?: "-"} mmHg",
            )
        }
    }
}

private fun formatRecord(
    record: com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord?,
    formatter: DateTimeFormatter,
): String {
    if (record == null) return "-"
    val dateText = formatter.format(Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()))
    return "${record.systolic}/${record.diastolic} em $dateText"
}
