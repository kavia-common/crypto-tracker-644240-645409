package org.example.app.data.local

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.example.app.data.local.dao.CoinDao
import org.example.app.utils.NetworkUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    private val database: AppDatabase,
    private val coinDao: CoinDao,
    private val networkUtils: NetworkUtils
) {
    private val cacheTimeout = TimeUnit.MINUTES.toMillis(5) // 5 minutes cache timeout

    suspend fun shouldRefreshCache(): Boolean {
        val coins = coinDao.getCoins().first()
        if (coins.isEmpty()) return true

        val lastUpdated = coins.maxOfOrNull { it.lastUpdated } ?: 0L
        val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdated
        
        return timeSinceLastUpdate > cacheTimeout
    }

    suspend fun invalidateCache() {
        database.withTransaction {
            coinDao.deleteAllCoins()
        }
    }

    suspend fun clearOldCache() {
        database.withTransaction {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            coinDao.deleteOldCoins(cutoffTime)
        }
    }
}
