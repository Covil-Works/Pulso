package com.covildev.pulso.feature_relatorio.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_relatorio.domain.model.ReportSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class PdfReportGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun generate(
        profile: UserProfile?,
        records: List<BloodPressureRecord>,
        summary: ReportSummary,
    ): File {
        val reportsFolder = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsFolder, "pulso_relatorio_${System.currentTimeMillis()}.pdf")

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val sectionPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }

        val dateTimeFormatter = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-BR"),
        )

        val document = PdfDocument()
        var pageIndex = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
        var currentPage = document.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var y = 40f

        fun nextPage() {
            document.finishPage(currentPage)
            pageIndex += 1
            currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            currentPage = document.startPage(currentPageInfo)
            canvas = currentPage.canvas
            y = 40f
        }

        fun drawLine(text: String, paint: Paint = bodyPaint, space: Float = 20f) {
            if (y > PAGE_HEIGHT - 40f) {
                nextPage()
            }
            canvas.drawText(text, 40f, y, paint)
            y += space
        }

        fun drawWrappedLine(
            text: String,
            paint: Paint = bodyPaint,
            space: Float = 20f,
            maxCharsPerLine: Int = 74,
        ) {
            if (text.length <= maxCharsPerLine) {
                drawLine(text, paint, space)
                return
            }
            val words = text.split(" ")
            var currentLine = ""
            words.forEach { word ->
                val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
                if (candidate.length <= maxCharsPerLine) {
                    currentLine = candidate
                } else {
                    if (currentLine.isNotBlank()) {
                        drawLine(currentLine, paint, space)
                    }
                    currentLine = word
                }
            }
            if (currentLine.isNotBlank()) {
                drawLine(currentLine, paint, space)
            }
        }

        val nowFormatted = dateTimeFormatter.format(Instant.now().atZone(ZoneId.systemDefault()))
        val patientName = profile?.name ?: "Não informado"
        val patientAge = profile?.age?.toString() ?: "-"
        val patientAdditionalInfo = profile?.additionalInfo
            ?.takeIf { it.isNotBlank() }
            ?: "Não informado"

        drawLine("Relatório de Pressão Arterial", titlePaint, 26f)
        drawLine("Paciente: $patientName | Idade: $patientAge")
        drawWrappedLine("Informações adicionais: $patientAdditionalInfo")
        drawLine("Gerado em: $nowFormatted")
        drawLine("")

        drawLine("Resumo Estatistico", sectionPaint, 24f)
        val highest = summary.highestRecord
        val lowest = summary.lowestRecord
        val averageSystolic = summary.averageSystolic?.toString() ?: "-"
        val averageDiastolic = summary.averageDiastolic?.toString() ?: "-"

        drawLine("Pico máximo: ${formatRecord(highest, dateTimeFormatter)}")
        drawLine("Pico mínimo: ${formatRecord(lowest, dateTimeFormatter)}")
        drawLine("Pressão média: $averageSystolic/$averageDiastolic mmHg")
        drawLine("")

        drawLine("Histórico detalhado", sectionPaint, 24f)
        if (records.isEmpty()) {
            drawLine("Nenhum registro encontrado no período.")
        } else {
            records.forEach { record ->
                val date = dateTimeFormatter.format(
                    Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()),
                )
                drawLine(
                    "$date | ${record.systolic}/${record.diastolic} mmHg | ${record.riskLevel.label}",
                )
                record.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    drawWrappedLine("Obs: ${note.take(MAX_NOTE_LENGTH)}")
                }
            }
        }

        document.finishPage(currentPage)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }
}

private fun formatRecord(
    record: BloodPressureRecord?,
    formatter: DateTimeFormatter,
): String {
    if (record == null) return "-"
    val date = formatter.format(Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()))
    return "${record.systolic}/${record.diastolic} mmHg em $date"
}

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MAX_NOTE_LENGTH = 80
