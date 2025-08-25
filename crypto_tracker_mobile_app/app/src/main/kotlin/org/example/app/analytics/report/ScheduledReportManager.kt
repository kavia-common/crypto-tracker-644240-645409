package org.example.app.analytics.report

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.app.analytics.AnalyticsFilterOptions
import org.example.app.analytics.AnalyticsTimeRange
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledReportManager @Inject constructor(
    private val context: Context,
    private val reportGenerator: AnalyticsReportGenerator,
    private val reportNotifier: ReportNotificationManager
) {
    private val _scheduledReports = MutableStateFlow<List<ScheduledReport>>(emptyList())
    val scheduledReports: StateFlow<List<ScheduledReport>> = _scheduledReports

    fun scheduleReport(
        schedule: ReportSchedule,
        timeRange: AnalyticsTimeRange,
        filters: AnalyticsFilterOptions,
        format: ReportFormat,
        notifyOnCompletion: Boolean
    ): String {
        val reportId = generateReportId()
        val workRequest = createWorkRequest(
            reportId,
            schedule,
            timeRange,
            filters,
            format,
            notifyOnCompletion
        )

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                reportId,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

        val scheduledReport = ScheduledReport(
            id = reportId,
            schedule = schedule,
            timeRange = timeRange,
            filters = filters,
            format = format,
            notifyOnCompletion = notifyOnCompletion
        )

        _scheduledReports.value = _scheduledReports.value + scheduledReport
        return reportId
    }

    fun cancelScheduledReport(reportId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(reportId)

        _scheduledReports.value = _scheduledReports.value.filter { it.id != reportId }
    }

    fun updateScheduledReport(
        reportId: String,
        schedule: ReportSchedule? = null,
        timeRange: AnalyticsTimeRange? = null,
        filters: AnalyticsFilterOptions? = null,
        format: ReportFormat? = null,
        notifyOnCompletion: Boolean? = null
    ) {
        val existingReport = _scheduledReports.value.find { it.id == reportId } ?: return
        
        cancelScheduledReport(reportId)
        
        scheduleReport(
            schedule ?: existingReport.schedule,
            timeRange ?: existingReport.timeRange,
            filters ?: existingReport.filters,
            format ?: existingReport.format,
            notifyOnCompletion ?: existingReport.notifyOnCompletion
        )
    }

    private fun createWorkRequest(
        reportId: String,
        schedule: ReportSchedule,
        timeRange: AnalyticsTimeRange,
        filters: AnalyticsFilterOptions,
        format: ReportFormat,
        notifyOnCompletion: Boolean
    ): PeriodicWorkRequest {
        val data = workDataOf(
            "report_id" to reportId,
            "time_range" to timeRange.name,
            "filters" to filters.toString(),
            "format" to format.name,
            "notify" to notifyOnCompletion
        )

        return PeriodicWorkRequestBuilder<ReportGenerationWorker>(
            schedule.interval,
            schedule.timeUnit,
            schedule.flexInterval,
            schedule.timeUnit
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(data)
            .build()
    }

    private fun generateReportId(): String {
        return "report_${System.currentTimeMillis()}"
    }
}

data class ScheduledReport(
    val id: String,
    val schedule: ReportSchedule,
    val timeRange: AnalyticsTimeRange,
    val filters: AnalyticsFilterOptions,
    val format: ReportFormat,
    val notifyOnCompletion: Boolean
)

data class ReportSchedule(
    val interval: Long,
    val flexInterval: Long,
    val timeUnit: TimeUnit
) {
    companion object {
        val DAILY = ReportSchedule(24, 1, TimeUnit.HOURS)
        val WEEKLY = ReportSchedule(7, 1, TimeUnit.DAYS)
        val MONTHLY = ReportSchedule(30, 1, TimeUnit.DAYS)
    }
}

enum class ReportFormat {
    HTML,
    PDF,
    MARKDOWN
}
