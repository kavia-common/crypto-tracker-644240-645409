package org.example.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.example.app.data.local.entities.CoinEntity

@Dao
interface CoinDao {
    @Query("SELECT * FROM coins ORDER BY marketCapRank")
    fun getCoins(): Flow<List<CoinEntity>>

    @Query("SELECT * FROM coins WHERE id = :coinId")
    suspend fun getCoin(coinId: String): CoinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    @Query("DELETE FROM coins")
    suspend fun deleteAllCoins()

    @Query("DELETE FROM coins WHERE lastUpdated < :timestamp")
    suspend fun deleteOldCoins(timestamp: Long)

    @Transaction
    suspend fun updateCoins(coins: List<CoinEntity>) {
        deleteAllCoins()
        insertCoins(coins)
    }

    @Query("SELECT * FROM coins WHERE id IN (:ids)")
    suspend fun getCoinsById(ids: List<String>): List<CoinEntity>

    @Query("SELECT COUNT(*) FROM coins")
    suspend fun getCoinCount(): Int
}
