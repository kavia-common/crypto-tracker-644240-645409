package org.example.app.analytics

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomatedCrashReporterService @Inject constructor(
    private val application: Application,
    private val crashReporter: CrashReporter,
    private val eventLogger: EventLogger
) {
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val crashReports = MutableStateFlow<List<CrashReport>>(emptyList())

    init {
        setupCrashHandler()
        monitorANRs()
        trackMemoryUsage()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val report = CrashReport(
            timestamp = System.currentTimeMillis(),
            threadName = thread.name,
            exception = throwable,
            deviceInfo = collectDeviceInfo()
        )
        scope.launch {
            saveCrashReport(report)
            uploadCrashReports()
        }
    }

    private fun monitorANRs() {
        scope.launch {
            val anrFile = File("/data/anr/traces.txt")
            if (anrFile.exists()) {
                anrFile.inputStream().bufferedReader().use { reader ->
                    val traces = reader.readText()
                    crashReporter.setCustomKey("anr_traces", traces)
                }
            }
        }
    }

    private fun trackMemoryUsage() {
        scope.launch {
            while (true) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                
                if (usedMemory > maxMemory * 0.85) { // 85% threshold
                    eventLogger.logError(
                        MemoryWarning("High memory usage detected"),
                        mapOf(
                            "used_memory" to usedMemory.toString(),
                            "max_memory" to maxMemory.toString()
                        )
                    )
                }
                kotlinx.coroutines.delay(60000) // Check every minute
            }
        }
    }

    private fun collectDeviceInfo(): Map<String, String> {
        return mapOf(
            "device_model" to android.os.Build.MODEL,
            "android_version" to android.os.Build.VERSION.RELEASE,
            "app_version" to application.packageManager
                .getPackageInfo(application.packageName, 0).versionName
        )
    }

    private suspend fun saveCrashReport(report: CrashReport) {
        crashReports.value = crashReports.value + report
    }

    private suspend fun uploadCrashReports() {
        crashReports.value.forEach { report ->
            crashReporter.setCustomKey("thread_name", report.threadName)
            report.deviceInfo.forEach { (key, value) ->
                crashReporter.setCustomKey(key, value)
            }
            crashReporter.logException(report.exception)
        }
        crashReports.value = emptyList()
    }

    data class CrashReport(
        val timestamp: Long,
        val threadName: String,
        val exception: Throwable,
        val deviceInfo: Map<String, String>
    )

    class MemoryWarning(message: String) : RuntimeException(message)
}
