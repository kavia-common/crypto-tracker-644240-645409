package org.example.app.analytics.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSharingService @Inject constructor(
    private val context: Context,
    private val reportArchiveManager: ReportArchiveManager
) {
    suspend fun shareReport(reportId: String, recipients: List<String>? = null) {
        val report = reportArchiveManager.getArchivedReport(reportId) ?: return
        val shareableUri = getShareableUri(report.uri)

        val shareIntent = createShareIntent(shareableUri, report.format, recipients)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(shareIntent)
    }

    suspend fun exportReport(reportId: String, destination: Uri) = withContext(Dispatchers.IO) {
        val report = reportArchiveManager.getArchivedReport(reportId) ?: return@withContext
        val sourceFile = File(report.uri.path!!)

        context.contentResolver.openOutputStream(destination)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun getShareableUri(uri: Uri): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(uri.path!!)
        )
    }

    private fun createShareIntent(
        uri: Uri,
        format: String,
        recipients: List<String>? = null
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(format)
            putExtra(Intent.EXTRA_STREAM, uri)
            
            recipients?.let { emails ->
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, "Analytics Report")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Please find attached the analytics report generated on ${
                        java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date())
                    }"
                )
            }
            
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getMimeType(format: String): String {
        return when (format.toLowerCase()) {
            "pdf" -> "application/pdf"
            "html" -> "text/html"
            "md" -> "text/markdown"
            else -> "*/*"
        }
    }
}
