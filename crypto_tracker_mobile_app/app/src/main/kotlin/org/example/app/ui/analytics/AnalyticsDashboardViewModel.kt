package org.example.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.app.analytics.EventLogger
import org.example.app.experiments.ExperimentManager
import javax.inject.Inject

class AnalyticsDashboardViewModel @Inject constructor(
    private val eventLogger: EventLogger,
    private val experimentManager: ExperimentManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsDashboardState>(AnalyticsDashboardState.Loading)
    val uiState: StateFlow<AnalyticsDashboardState> = _uiState

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                // Simulated analytics data
                val engagementData = generateEngagementData()
                val experimentResults = loadExperimentResults()
                val crashAnalytics = loadCrashAnalytics()

                _uiState.value = AnalyticsDashboardState.Success(
                    engagementData = engagementData,
                    experimentResults = experimentResults,
                    crashAnalytics = crashAnalytics
                )
            } catch (e: Exception) {
                _uiState.value = AnalyticsDashboardState.Error(e.message ?: "Failed to load analytics")
            }
        }
    }

    private fun generateEngagementData(): List<EngagementDataPoint> {
        // Simulate 7 days of engagement data
        return (0..6).map { daysAgo ->
            EngagementDataPoint(
                timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000),
                activeUsers = (100..500).random(),
                sessionDuration = (5..30).random() * 60L,
                screenViews = (300..1000).random()
            )
        }.reversed()
    }

    private fun loadExperimentResults(): List<ExperimentResult> {
        return listOf(
            ExperimentManager.EXPERIMENT_NEW_CHART_UI,
            ExperimentManager.EXPERIMENT_PORTFOLIO_LAYOUT,
            ExperimentManager.EXPERIMENT_PRICE_ALERT_THRESHOLD
        ).mapNotNull { experimentId ->
            if (experimentManager.isUserInExperiment(experimentId)) {
                ExperimentResult(
                    id = experimentId,
                    variant = experimentManager.getExperimentVariant(experimentId),
                    conversionRate = (10..30).random() / 100.0,
                    improvement = (-10..20).random() / 100.0
                )
            } else null
        }
    }

    private fun loadCrashAnalytics(): CrashAnalytics {
        return CrashAnalytics(
            crashRate = 0.5, // 0.5%
            crashTypes = mapOf(
                "NullPointerException" to 35,
                "NetworkError" to 25,
                "OutOfMemoryError" to 20,
                "IllegalStateException" to 15,
                "Other" to 5
            )
        )
    }
}

sealed class AnalyticsDashboardState {
    object Loading : AnalyticsDashboardState()
    data class Success(
        val engagementData: List<EngagementDataPoint>,
        val experimentResults: List<ExperimentResult>,
        val crashAnalytics: CrashAnalytics
    ) : AnalyticsDashboardState()
    data class Error(val message: String) : AnalyticsDashboardState()
}

data class EngagementDataPoint(
    val timestamp: Long,
    val activeUsers: Int,
    val sessionDuration: Long,
    val screenViews: Int
)

data class ExperimentResult(
    val id: String,
    val variant: String,
    val conversionRate: Double,
    val improvement: Double
)

data class CrashAnalytics(
    val crashRate: Double,
    val crashTypes: Map<String, Int>
)
