package org.example.app.analytics.report

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportArchiveManager @Inject constructor(
    private val context: Context,
    private val exportTemplate: AnalyticsExportTemplate
) {
    private val reportsDir: File by lazy {
        File(context.filesDir, "reports").apply { mkdirs() }
    }

    private val _archivedReports = MutableStateFlow<List<ArchivedReport>>(emptyList())
    val archivedReports: Flow<List<ArchivedReport>> = _archivedReports

    suspend fun saveHtmlReport(reportId: String, report: AnalyticsReport): Uri {
        val content = exportTemplate.generateHtmlReport(report)
        return saveReport(reportId, content, "html")
    }

    suspend fun savePdfReport(reportId: String, report: AnalyticsReport): Uri {
        val content = exportTemplate.generatePdfReport(report)
        return saveReport(reportId, content, "pdf", isBinary = true)
    }

    suspend fun saveMarkdownReport(reportId: String, report: AnalyticsReport): Uri {
        val content = exportTemplate.generateMarkdownReport(report)
        return saveReport(reportId, content, "md")
    }

    private fun saveReport(
        reportId: String,
        content: Any,
        extension: String,
        isBinary: Boolean = false
    ): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())
        
        val reportFile = File(reportsDir, "${reportId}_${timestamp}.$extension")

        when {
            isBinary && content is ByteArray -> reportFile.writeBytes(content)
            content is String -> reportFile.writeText(content)
            else -> throw IllegalArgumentException("Unsupported content type")
        }

        val archivedReport = ArchivedReport(
            id = reportId,
            timestamp = System.currentTimeMillis(),
            format = extension,
            uri = Uri.fromFile(reportFile)
        )

        _archivedReports.value = _archivedReports.value + archivedReport

        return Uri.fromFile(reportFile)
    }

    fun getArchivedReport(reportId: String): ArchivedReport? {
        return _archivedReports.value.find { it.id == reportId }
    }

    fun deleteReport(reportId: String) {
        _archivedReports.value.find { it.id == reportId }?.let { report ->
            File(report.uri.path!!).delete()
            _archivedReports.value = _archivedReports.value - report
        }
    }

    fun cleanOldReports(maxAgeDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        val oldReports = _archivedReports.value.filter { it.timestamp < cutoffTime }
        
        oldReports.forEach { report ->
            deleteReport(report.id)
        }
    }
}

data class ArchivedReport(
    val id: String,
    val timestamp: Long,
    val format: String,
    val uri: Uri
)
