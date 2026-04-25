package com.covildev.pulso.feature_relatorio.domain.model

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord

data class ReportSummary(
    val highestRecord: BloodPressureRecord?,
    val lowestRecord: BloodPressureRecord?,
    val averageSystolic: Int?,
    val averageDiastolic: Int?,
)
