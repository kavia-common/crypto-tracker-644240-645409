package org.example.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventLogger @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashReporter: CrashReporter
) {
    fun logUserEngagement(eventType: String, duration: Long) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, eventType)
            putLong(PARAM_DURATION, duration)
        }
        analytics.logEvent(EVENT_USER_ENGAGEMENT, bundle)
    }

    fun logFeatureUsage(featureName: String, result: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, featureName)
            putString(PARAM_RESULT, result)
        }
        analytics.logEvent(EVENT_FEATURE_USAGE, bundle)
    }

    fun logUserAction(action: String, target: String, extra: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            putString(PARAM_ACTION, action)
            putString(PARAM_TARGET, target)
            extra.forEach { (key, value) ->
                putString(key, value)
            }
        }
        analytics.logEvent(EVENT_USER_ACTION, bundle)
    }

    fun logError(error: Throwable, context: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            putString(PARAM_ERROR_TYPE, error.javaClass.simpleName)
            putString(PARAM_ERROR_MESSAGE, error.message)
            context.forEach { (key, value) ->
                putString(key, value)
            }
        }
        analytics.logEvent(EVENT_ERROR, bundle)
        crashReporter.logNonFatal(error, context)
    }

    fun logPerformanceMetric(name: String, value: Long, context: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            putString(PARAM_METRIC_NAME, name)
            putLong(PARAM_METRIC_VALUE, value)
            context.forEach { (key, value) ->
                putString(key, value)
            }
        }
        analytics.logEvent(EVENT_PERFORMANCE_METRIC, bundle)
    }

    companion object {
        private const val EVENT_USER_ENGAGEMENT = "user_engagement"
        private const val EVENT_FEATURE_USAGE = "feature_usage"
        private const val EVENT_USER_ACTION = "user_action"
        private const val EVENT_ERROR = "error"
        private const val EVENT_PERFORMANCE_METRIC = "performance_metric"

        private const val PARAM_DURATION = "duration"
        private const val PARAM_RESULT = "result"
        private const val PARAM_ACTION = "action"
        private const val PARAM_TARGET = "target"
        private const val PARAM_ERROR_TYPE = "error_type"
        private const val PARAM_ERROR_MESSAGE = "error_message"
        private const val PARAM_METRIC_NAME = "metric_name"
        private const val PARAM_METRIC_VALUE = "metric_value"
    }
}
