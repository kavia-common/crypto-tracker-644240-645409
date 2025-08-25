package org.example.app.data.repository

import kotlinx.coroutines.flow.*
import org.example.app.data.api.CoinApiService
import org.example.app.data.api.WebSocketService
import org.example.app.data.local.dao.CoinDao
import org.example.app.data.local.entities.CoinEntity
import org.example.app.data.model.Coin
import org.example.app.utils.NetworkUtils
import org.example.app.utils.Result
import org.example.app.utils.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(
    private val apiService: CoinApiService,
    private val webSocketService: WebSocketService,
    private val coinDao: CoinDao,
    private val networkUtils: NetworkUtils
) {
    val coins: Flow<Result<List<Coin>>> = coinDao.getCoins()
        .map { entities -> Result.Success(entities.map { it.toCoin() }) as Result<List<Coin>> }
        .catch { e -> emit(Result.Error(Exception(e))) }

    suspend fun refreshCoins(): Result<Unit> {
        return if (networkUtils.isNetworkAvailable()) {
            apiCall {
                val fetchedCoins = apiService.getCoins()
                coinDao.updateCoins(fetchedCoins.map { CoinEntity.fromCoin(it) })
            }
        } else {
            Result.Error(Exception("No internet connection"))
        }
    }

    suspend fun startPriceUpdates() {
        try {
            // Initial refresh
            refreshCoins()

            if (networkUtils.isNetworkAvailable()) {
                // Start WebSocket connection
                val currentCoins = coinDao.getCoins().first()
                val symbols = currentCoins.map { it.symbol.lowercase() + "usdt" }
                webSocketService.connect(symbols)

                // Collect WebSocket updates
                webSocketService.getPriceUpdates()
                    .catch { e -> 
                        // Handle WebSocket errors
                        reconnectWebSocket(symbols)
                    }
                    .collect { update ->
                        updateCoinPrice(update.symbol, update.price, update.change24h)
                    }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun reconnectWebSocket(symbols: List<String>) {
        webSocketService.disconnect()
        if (networkUtils.isNetworkAvailable()) {
            webSocketService.connect(symbols)
        }
    }

    private suspend fun updateCoinPrice(symbol: String, price: Double, change24h: Double) {
        val coin = coinDao.getCoins().first().find { 
            it.symbol.equals(symbol.removeSuffix("usdt"), ignoreCase = true) 
        }
        
        if (coin != null) {
            coinDao.insertCoins(listOf(coin.copy(
                currentPrice = price,
                priceChangePercentage24h = change24h,
                lastUpdated = System.currentTimeMillis()
            )))
        }
    }

    fun getCoinChart(id: String, days: Int): Flow<Result<ChartData>> = flow {
        emit(Result.Loading)
        if (networkUtils.isNetworkAvailable()) {
            emit(apiCall { apiService.getCoinChart(id, days = days) })
        } else {
            emit(Result.Error(Exception("No internet connection")))
        }
    }

    fun getPrices(ids: List<String>): Flow<Result<Map<String, Map<String, Double>>>> = flow {
        emit(Result.Loading)
        if (networkUtils.isNetworkAvailable()) {
            emit(apiCall { apiService.getPrices(ids.joinToString(",")) })
        } else {
            emit(Result.Error(Exception("No internet connection")))
        }
    }

    fun stopPriceUpdates() {
        webSocketService.disconnect()
    }

    companion object {
        private const val CACHE_TIMEOUT = 5 * 60 * 1000L // 5 minutes
    }
}
