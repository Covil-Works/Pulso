package com.covildev.pulso.feature_relatorio.domain.model

import android.net.Uri

data class GeneratedReport(
    val uri: Uri,
    val fileName: String,
    val summary: ReportSummary,
)
