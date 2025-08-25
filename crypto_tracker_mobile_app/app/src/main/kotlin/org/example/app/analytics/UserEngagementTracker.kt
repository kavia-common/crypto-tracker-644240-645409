package org.example.app.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserEngagementTracker @Inject constructor(
    private val eventLogger: EventLogger
) : Application.ActivityLifecycleCallbacks, LifecycleEventObserver {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val screenTimeMap = mutableMapOf<String, Long>()
    private val currentScreen = MutableStateFlow<String?>(null)
    private var sessionStartTime: Long = 0
    private var lastInteractionTime: Long = 0

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        // Track activity creation
    }

    override fun onActivityStarted(activity: Activity) {
        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()
            logSessionStart()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val screenName = activity.javaClass.simpleName
        currentScreen.value = screenName
        screenTimeMap[screenName] = System.currentTimeMillis()
    }

    override fun onActivityPaused(activity: Activity) {
        val screenName = activity.javaClass.simpleName
        screenTimeMap[screenName]?.let { startTime ->
            val duration = System.currentTimeMillis() - startTime
            logScreenEngagement(screenName, duration)
        }
        currentScreen.value = null
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isFinishing) {
            logSessionEnd()
            sessionStartTime = 0
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Track activity destruction
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // Track state save
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                lastInteractionTime = System.currentTimeMillis()
            }
            Lifecycle.Event.ON_PAUSE -> {
                logEngagementMetrics()
            }
            else -> {}
        }
    }

    fun trackUserInteraction(action: String, target: String) {
        lastInteractionTime = System.currentTimeMillis()
        eventLogger.logUserAction(
            action = action,
            target = target,
            mapOf(
                "screen" to (currentScreen.value ?: "unknown"),
                "session_duration" to getSessionDuration().toString()
            )
        )
    }

    private fun logSessionStart() {
        eventLogger.logUserEngagement(
            eventType = "session_start",
            duration = 0
        )
    }

    private fun logSessionEnd() {
        val duration = getSessionDuration()
        eventLogger.logUserEngagement(
            eventType = "session_end",
            duration = duration
        )
    }

    private fun logScreenEngagement(screenName: String, duration: Long) {
        eventLogger.logUserEngagement(
            eventType = "screen_view",
            duration = duration
        )
    }

    private fun logEngagementMetrics() {
        scope.launch {
            val sessionDuration = getSessionDuration()
            val timeFromLastInteraction = System.currentTimeMillis() - lastInteractionTime

            eventLogger.logPerformanceMetric(
                name = "session_duration",
                value = sessionDuration,
                mapOf(
                    "active_time" to (sessionDuration - timeFromLastInteraction).toString(),
                    "inactive_time" to timeFromLastInteraction.toString()
                )
            )
        }
    }

    private fun getSessionDuration(): Long {
        return if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else 0
    }

    companion object {
        private const val INACTIVE_THRESHOLD = 300000L // 5 minutes
    }
}
