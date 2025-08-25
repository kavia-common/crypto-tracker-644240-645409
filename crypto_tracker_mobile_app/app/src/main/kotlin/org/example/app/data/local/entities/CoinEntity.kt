package org.example.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.example.app.data.model.Coin

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val currentPrice: Double,
    val marketCap: Double,
    val marketCapRank: Int,
    val priceChangePercentage24h: Double,
    val volume24h: Double,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toCoin() = Coin(
        id = id,
        symbol = symbol,
        name = name,
        image = image,
        currentPrice = currentPrice,
        marketCap = marketCap,
        marketCapRank = marketCapRank,
        priceChangePercentage24h = priceChangePercentage24h,
        volume24h = volume24h
    )

    companion object {
        fun fromCoin(coin: Coin) = CoinEntity(
            id = coin.id,
            symbol = coin.symbol,
            name = coin.name,
            image = coin.image,
            currentPrice = coin.currentPrice,
            marketCap = coin.marketCap,
            marketCapRank = coin.marketCapRank,
            priceChangePercentage24h = coin.priceChangePercentage24h,
            volume24h = coin.volume24h
        )
    }
}
