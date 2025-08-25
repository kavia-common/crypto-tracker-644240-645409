package org.example.app.utils

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

object RetryHelper {
    private const val MAX_RETRIES = 3
    private const val INITIAL_DELAY = 1000L // 1 second
    private const val MAX_DELAY = 10000L // 10 seconds

    suspend fun <T> retry(
        times: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_DELAY,
        maxDelay: Long = MAX_DELAY,
        shouldRetry: (Exception) -> Boolean = { true },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == times - 1 || !shouldRetry(e)) {
                    throw e
                }
                delay(currentDelay)
                currentDelay = min(currentDelay * 2, maxDelay)
            }
        }
        throw IllegalStateException("Should never reach this point")
    }

    suspend fun <T> exponentialBackoff(
        times: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_DELAY,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == times - 1) {
                    throw e
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        throw IllegalStateException("Should never reach this point")
    }
}
