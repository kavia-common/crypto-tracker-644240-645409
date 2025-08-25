package org.example.app.analytics

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsExportService @Inject constructor(
    private val context: Context,
    private val analyticsDataManager: AnalyticsDataManager
) {
    private val gson = Gson()
    
    suspend fun exportAnalytics(
        timeRange: AnalyticsTimeRange,
        format: ExportFormat
    ): Uri = withContext(Dispatchers.IO) {
        val data = analyticsDataManager.getAnalyticsData(timeRange)
            .collect { it }
            
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())
        
        val fileName = "analytics_export_${timestamp}.${format.extension}"
        val file = File(context.cacheDir, fileName)

        when (format) {
            ExportFormat.JSON -> exportToJson(file, data)
            ExportFormat.CSV -> exportToCsv(file, data)
        }

        return@withContext Uri.fromFile(file)
    }

    private fun exportToJson(file: File, data: AnalyticsData) {
        file.writeText(gson.toJson(data))
    }

    private fun exportToCsv(file: File, data: AnalyticsData) {
        val csv = StringBuilder()
        
        // Add headers
        csv.appendLine("Date,Daily Active Users,Session Duration,Screen Views")
        
        // Add engagement metrics
        data.engagementMetrics.dailyActiveUsers.forEachIndexed { index, metric ->
            csv.appendLine(
                "${formatDate(metric.timestamp)}," +
                "${metric.value}," +
                "${data.engagementMetrics.sessionDuration[index].value}," +
                "${data.engagementMetrics.screenViews[index].value}"
            )
        }
        
        // Add experiment results
        csv.appendLine("\nExperiment Results")
        csv.appendLine("Experiment ID,Variant,Conversion Rate,Improvement,Confidence")
        data.experimentResults.forEach { experiment ->
            csv.appendLine(
                "${experiment.experimentId}," +
                "${experiment.variant}," +
                "${experiment.conversionRate}," +
                "${experiment.improvement}," +
                "${experiment.confidence}"
            )
        }
        
        // Add crash analytics
        csv.appendLine("\nCrash Analytics")
        csv.appendLine("Crash Rate: ${data.crashAnalytics.crashRate}")
        csv.appendLine("\nCrash Types")
        data.crashAnalytics.crashTypes.forEach { (type, count) ->
            csv.appendLine("$type,$count")
        }
        
        file.writeText(csv.toString())
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Date(timestamp))
    }
}

enum class ExportFormat(val extension: String) {
    JSON("json"),
    CSV("csv")
}
