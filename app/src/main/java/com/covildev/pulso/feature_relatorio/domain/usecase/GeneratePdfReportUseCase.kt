package com.covildev.pulso.feature_relatorio.domain.usecase

import android.content.Context
import androidx.core.content.FileProvider
import com.covildev.pulso.feature_perfil.domain.repository.ProfileRepository
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import com.covildev.pulso.feature_relatorio.data.PdfReportGenerator
import com.covildev.pulso.feature_relatorio.domain.model.GeneratedReport
import com.covildev.pulso.feature_relatorio.domain.model.ReportSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GeneratePdfReportUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val pdfReportGenerator: PdfReportGenerator,
) {
    suspend operator fun invoke(): Result<GeneratedReport> {
        return runCatching {
            val profile = profileRepository.getProfile()
            val records = bloodPressureRepository.getAllRecords()
                .sortedByDescending { it.timestamp }
            val summary = buildSummary(records)
            val file = pdfReportGenerator.generate(profile = profile, records = records, summary = summary)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            GeneratedReport(
                uri = uri,
                fileName = file.name,
                summary = summary,
            )
        }
    }
}

private fun buildSummary(records: List<BloodPressureRecord>): ReportSummary {
    val highest = records.maxWithOrNull(compareBy<BloodPressureRecord> { it.systolic }.thenBy { it.diastolic })
    val lowest = records.minWithOrNull(compareBy<BloodPressureRecord> { it.systolic }.thenBy { it.diastolic })
    val averageSystolic = records.takeIf { it.isNotEmpty() }?.map { it.systolic }?.average()?.toInt()
    val averageDiastolic = records.takeIf { it.isNotEmpty() }?.map { it.diastolic }?.average()?.toInt()
    return ReportSummary(
        highestRecord = highest,
        lowestRecord = lowest,
        averageSystolic = averageSystolic,
        averageDiastolic = averageDiastolic,
    )
}
