package org.example.app.analytics.report

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.example.app.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportNotificationManager @Inject constructor(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    fun notifyReportGenerated(reportId: String, reportUri: Uri) {
        val viewIntent = createViewIntent(reportUri)
        val shareIntent = createShareIntent(reportUri)

        val viewPendingIntent = PendingIntent.getActivity(
            context,
            0,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sharePendingIntent = PendingIntent.getActivity(
            context,
            1,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Analytics Report Ready")
            .setContentText("Your scheduled analytics report is now available")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(viewPendingIntent)
            .addAction(
                R.drawable.ic_share,
                "Share",
                sharePendingIntent
            )
            .build()

        notificationManager.notify(reportId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Analytics Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for analytics report generation"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createViewIntent(reportUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(reportUri, getMimeType(reportUri))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun createShareIntent(reportUri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(reportUri)
            putExtra(Intent.EXTRA_STREAM, reportUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun getMimeType(uri: Uri): String {
        return when (uri.lastPathSegment?.substringAfterLast('.')) {
            "pdf" -> "application/pdf"
            "html" -> "text/html"
            "md" -> "text/markdown"
            else -> "*/*"
        }
    }

    companion object {
        private const val CHANNEL_ID = "analytics_reports"
    }
}
