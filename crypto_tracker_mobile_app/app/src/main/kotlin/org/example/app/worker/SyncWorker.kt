package org.example.app.worker

import android.content.Context
import androidx.work.*
import org.example.app.data.repository.CoinRepository
import org.example.app.utils.NetworkUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: CoinRepository,
    private val networkUtils: NetworkUtils
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                repository.refreshCoins()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "SyncWorker"

        fun start(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
