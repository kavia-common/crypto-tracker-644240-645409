package org.example.app.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun updatePriceAlerts(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_PRICE_ALERTS, enabled)
        }
        updateSubscription("price_alerts", enabled)
    }

    suspend fun updateNewsAlerts(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_NEWS_ALERTS, enabled)
        }
        updateSubscription("news_alerts", enabled)
    }

    private suspend fun updateSubscription(topic: String, subscribe: Boolean) {
        try {
            if (subscribe) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun isPriceAlertsEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRICE_ALERTS, true)
    }

    fun isNewsAlertsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NEWS_ALERTS, true)
    }

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_PRICE_ALERTS = "price_alerts_enabled"
        private const val KEY_NEWS_ALERTS = "news_alerts_enabled"
    }
}
