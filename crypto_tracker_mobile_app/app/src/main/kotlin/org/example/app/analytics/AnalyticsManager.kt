package org.example.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun logScreenView(screenName: String, screenClass: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    fun logCoinView(coinId: String, coinName: String) {
        firebaseAnalytics.logEvent(COIN_VIEW_EVENT) {
            param(FirebaseAnalytics.Param.ITEM_ID, coinId)
            param(FirebaseAnalytics.Param.ITEM_NAME, coinName)
        }
    }

    fun logPortfolioAction(action: String, coinId: String) {
        firebaseAnalytics.logEvent(PORTFOLIO_ACTION_EVENT) {
            param(PARAM_ACTION_TYPE, action)
            param(FirebaseAnalytics.Param.ITEM_ID, coinId)
        }
    }

    fun logWatchlistAction(action: String, coinId: String) {
        firebaseAnalytics.logEvent(WATCHLIST_ACTION_EVENT) {
            param(PARAM_ACTION_TYPE, action)
            param(FirebaseAnalytics.Param.ITEM_ID, coinId)
        }
    }

    fun logError(error: Throwable, details: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            putString(PARAM_ERROR_TYPE, error.javaClass.simpleName)
            putString(PARAM_ERROR_MESSAGE, error.message)
            details.forEach { (key, value) ->
                putString(key, value)
            }
        }
        firebaseAnalytics.logEvent(ERROR_EVENT, bundle)
    }

    companion object {
        private const val COIN_VIEW_EVENT = "coin_view"
        private const val PORTFOLIO_ACTION_EVENT = "portfolio_action"
        private const val WATCHLIST_ACTION_EVENT = "watchlist_action"
        private const val ERROR_EVENT = "app_error"
        
        private const val PARAM_ACTION_TYPE = "action_type"
        private const val PARAM_ERROR_TYPE = "error_type"
        private const val PARAM_ERROR_MESSAGE = "error_message"
    }
}
