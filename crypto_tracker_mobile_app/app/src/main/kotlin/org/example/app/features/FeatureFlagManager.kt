package org.example.app.features

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class FeatureFlagManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    init {
        setupDefaultConfig()
        fetchConfig()
    }

    private fun setupDefaultConfig() {
        val defaults = mapOf(
            FEATURE_CHARTS to true,
            FEATURE_PORTFOLIO to true,
            FEATURE_WATCHLIST to true,
            FEATURE_PRICE_ALERTS to false,
            FEATURE_NEWS to false,
            PRICE_UPDATE_INTERVAL to 15000L, // 15 seconds
            CACHE_DURATION to 3600000L // 1 hour
        )
        
        remoteConfig.setDefaultsAsync(defaults)
    }

    private fun fetchConfig() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Config params updated
                } else {
                    // Error fetching config
                }
            }
    }

    fun isFeatureEnabled(feature: String): Boolean {
        return remoteConfig.getBoolean(feature)
    }

    fun getString(key: String): String {
        return remoteConfig.getString(key)
    }

    fun getLong(key: String): Long {
        return remoteConfig.getLong(key)
    }

    fun getDouble(key: String): Double {
        return remoteConfig.getDouble(key)
    }

    companion object {
        const val FEATURE_CHARTS = "feature_charts"
        const val FEATURE_PORTFOLIO = "feature_portfolio"
        const val FEATURE_WATCHLIST = "feature_watchlist"
        const val FEATURE_PRICE_ALERTS = "feature_price_alerts"
        const val FEATURE_NEWS = "feature_news"
        const val PRICE_UPDATE_INTERVAL = "price_update_interval"
        const val CACHE_DURATION = "cache_duration"
    }
}
