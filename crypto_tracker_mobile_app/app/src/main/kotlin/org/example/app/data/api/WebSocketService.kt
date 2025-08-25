package org.example.app.data.api

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketService @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val priceUpdateChannel = Channel<PriceUpdate>(Channel.BUFFERED)

    fun connect(symbols: List<String>) {
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/ws/${symbols.joinToString(",")}@ticker")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                parsePriceUpdate(text)?.let { update ->
                    priceUpdateChannel.trySend(update)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Implement reconnection logic
                reconnect(symbols)
            }
        })
    }

    private fun reconnect(symbols: List<String>) {
        disconnect()
        connect(symbols)
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }

    fun getPriceUpdates(): Flow<PriceUpdate> = priceUpdateChannel.receiveAsFlow()

    private fun parsePriceUpdate(text: String): PriceUpdate? {
        return try {
            // Parse WebSocket message to PriceUpdate object
            // This is a simplified example, actual implementation would parse JSON
            val parts = text.split(",")
            PriceUpdate(
                symbol = parts[0],
                price = parts[1].toDouble(),
                change24h = parts[2].toDouble()
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class PriceUpdate(
    val symbol: String,
    val price: Double,
    val change24h: Double
)
