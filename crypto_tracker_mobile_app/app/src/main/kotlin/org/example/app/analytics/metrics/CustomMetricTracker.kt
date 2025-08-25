package org.example.app.analytics.metrics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.example.app.analytics.EventLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomMetricTracker @Inject constructor(
    private val eventLogger: EventLogger
) {
    private val metrics = ConcurrentHashMap<String, CustomMetric>()
    private val metricUpdates = MutableStateFlow<Map<String, CustomMetricValue>>(emptyMap())

    fun registerMetric(
        name: String,
        type: MetricType,
        description: String,
        aggregation: AggregationType = AggregationType.SUM
    ) {
        metrics[name] = CustomMetric(
            name = name,
            type = type,
            description = description,
            aggregation = aggregation
        )
    }

    fun trackMetric(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        val metric = metrics[name] ?: run {
            eventLogger.logError(
                IllegalArgumentException("Metric $name not registered"),
                mapOf("context" to "custom_metric_tracking")
            )
            return
        }

        val currentValue = metricUpdates.value[name]
        val newValue = when (metric.aggregation) {
            AggregationType.SUM -> CustomMetricValue(
                value = (currentValue?.value ?: 0.0) + value,
                count = (currentValue?.count ?: 0) + 1,
                tags = mergeTags(currentValue?.tags, tags)
            )
            AggregationType.AVERAGE -> CustomMetricValue(
                value = value,
                count = (currentValue?.count ?: 0) + 1,
                tags = mergeTags(currentValue?.tags, tags)
            )
            AggregationType.MAX -> CustomMetricValue(
                value = maxOf(currentValue?.value ?: Double.NEGATIVE_INFINITY, value),
                count = (currentValue?.count ?: 0) + 1,
                tags = mergeTags(currentValue?.tags, tags)
            )
            AggregationType.MIN -> CustomMetricValue(
                value = minOf(currentValue?.value ?: Double.POSITIVE_INFINITY, value),
                count = (currentValue?.count ?: 0) + 1,
                tags = mergeTags(currentValue?.tags, tags)
            )
        }

        metricUpdates.value = metricUpdates.value + (name to newValue)
    }

    fun getMetricValue(name: String): Flow<Double> {
        return metricUpdates.map { updates ->
            val value = updates[name]
            when (metrics[name]?.aggregation) {
                AggregationType.AVERAGE -> value?.let { it.value / it.count } ?: 0.0
                else -> value?.value ?: 0.0
            }
        }
    }

    fun getMetricWithTags(name: String): Flow<CustomMetricValue> {
        return metricUpdates.map { updates ->
            updates[name] ?: CustomMetricValue(0.0, 0, emptyMap())
        }
    }

    fun resetMetric(name: String) {
        metricUpdates.value = metricUpdates.value - name
    }

    fun resetAllMetrics() {
        metricUpdates.value = emptyMap()
    }

    private fun mergeTags(
        existing: Map<String, String>?,
        new: Map<String, String>
    ): Map<String, String> {
        return (existing ?: emptyMap()) + new
    }
}

data class CustomMetric(
    val name: String,
    val type: MetricType,
    val description: String,
    val aggregation: AggregationType
)

data class CustomMetricValue(
    val value: Double,
    val count: Int,
    val tags: Map<String, String>
)

enum class MetricType {
    COUNTER,
    GAUGE,
    HISTOGRAM
}

enum class AggregationType {
    SUM,
    AVERAGE,
    MAX,
    MIN
}
