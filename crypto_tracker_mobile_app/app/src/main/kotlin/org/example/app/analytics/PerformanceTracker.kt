package org.example.app.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceTracker @Inject constructor(
    private val performance: FirebasePerformance
) {
    fun startTrace(traceName: String): Trace {
        return performance.newTrace(traceName).apply {
            start()
        }
    }

    suspend fun <T> measureAsync(traceName: String, block: suspend () -> T): T {
        val trace = startTrace(traceName)
        try {
            return block()
        } finally {
            trace.stop()
        }
    }

    fun incrementCounter(trace: Trace, counterName: String) {
        trace.incrementMetric(counterName, 1)
    }

    fun putMetric(trace: Trace, metricName: String, value: Long) {
        trace.putMetric(metricName, value)
    }

    fun addAttribute(trace: Trace, attributeName: String, value: String) {
        trace.putAttribute(attributeName, value)
    }

    companion object {
        const val TRACE_NETWORK_REQUEST = "network_request"
        const val TRACE_DATABASE_OPERATION = "database_operation"
        const val TRACE_SCREEN_LOAD = "screen_load"
        const val TRACE_CHART_RENDER = "chart_render"
    }
}
