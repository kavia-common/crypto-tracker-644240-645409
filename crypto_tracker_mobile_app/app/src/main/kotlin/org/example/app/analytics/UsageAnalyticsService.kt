package org.example.app.analytics

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageAnalyticsService @Inject constructor(
    application: Application,
    private val eventLogger: EventLogger
) : LifecycleEventObserver {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val sessionStart = MutableStateFlow<Long>(0)
    private var isFirstLaunch = true

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        trackAppUsageStats()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (isFirstLaunch) {
                    logFirstLaunch()
                    isFirstLaunch = false
                }
                sessionStart.value = System.currentTimeMillis()
            }
            Lifecycle.Event.ON_STOP -> {
                logSessionEnd()
            }
            else -> {}
        }
    }

    private fun trackAppUsageStats() {
        scope.launch {
            sessionStart.collectLatest { startTime ->
                if (startTime > 0) {
                    val duration = System.currentTimeMillis() - startTime
                    eventLogger.logUserEngagement("session", duration)
                }
            }
        }
    }

    private fun logFirstLaunch() {
        eventLogger.logUserAction(
            action = "app_first_launch",
            target = "app"
        )
    }

    private fun logSessionEnd() {
        val startTime = sessionStart.value
        if (startTime > 0) {
            val sessionDuration = System.currentTimeMillis() - startTime
            eventLogger.logUserEngagement(
                eventType = "session_end",
                duration = sessionDuration
            )
            sessionStart.value = 0
        }
    }

    fun trackScreenView(screenName: String, duration: Long) {
        eventLogger.logUserEngagement(
            eventType = "screen_view",
            duration = duration
        )
    }

    fun trackFeatureUsage(feature: String, result: String) {
        eventLogger.logFeatureUsage(feature, result)
    }

    fun trackUserAction(action: String, target: String, details: Map<String, String> = emptyMap()) {
        eventLogger.logUserAction(action, target, details)
    }
}
