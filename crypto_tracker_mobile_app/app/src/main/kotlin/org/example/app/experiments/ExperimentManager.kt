package org.example.app.experiments

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.example.app.analytics.EventLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val eventLogger: EventLogger
) {
    fun getExperimentVariant(experimentId: String): String {
        val variant = remoteConfig.getString("experiment_${experimentId}_variant")
        logExperimentExposure(experimentId, variant)
        return variant
    }

    fun isUserInExperiment(experimentId: String): Boolean {
        return remoteConfig.getBoolean("experiment_${experimentId}_enabled")
    }

    fun logExperimentExposure(experimentId: String, variant: String) {
        eventLogger.logUserAction(
            action = "experiment_exposure",
            target = experimentId,
            mapOf("variant" to variant)
        )
    }

    fun logExperimentConversion(experimentId: String, conversionType: String) {
        val variant = getExperimentVariant(experimentId)
        eventLogger.logUserAction(
            action = "experiment_conversion",
            target = experimentId,
            mapOf(
                "variant" to variant,
                "conversion_type" to conversionType
            )
        )
    }

    companion object {
        const val EXPERIMENT_NEW_CHART_UI = "new_chart_ui"
        const val EXPERIMENT_PORTFOLIO_LAYOUT = "portfolio_layout"
        const val EXPERIMENT_PRICE_ALERT_THRESHOLD = "price_alert_threshold"
    }
}
