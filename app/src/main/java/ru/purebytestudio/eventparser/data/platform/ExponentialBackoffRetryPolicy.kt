package ru.purebytestudio.eventparser.data.platform

import kotlinx.coroutines.delay
import retrofit2.HttpException
import ru.purebytestudio.eventparser.platform.RetryPolicy
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ExponentialBackoffRetryPolicy(
    private val maxAttempts: Int = 3,
    private val initialDelay: Long = 1_000L,
    private val maxDelay: Long = 30_000L,
    private val factor: Double = 2.0
) : RetryPolicy {
    override suspend fun <T> execute(block: suspend () -> T): T {
        var currentDelay = initialDelay

        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val isLastAttempt = attempt == maxAttempts - 1
                if (isLastAttempt || !isRetryable(e)) {
                    throw e
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }

        error("Retry policy reached unexpected state")
    }

    private fun isRetryable(e: Exception): Boolean {
        return when (e) {
            is IOException -> when (e) {
                is SocketTimeoutException, is UnknownHostException -> true
                else -> e.message?.contains(
                    other = "timeout",
                    ignoreCase = true
                ) == true || e.message?.contains(
                    other = "connection",
                    ignoreCase = true
                ) == true
            }

            is HttpException -> {
                val code = e.code()
                code == 408 || code == 429 || code in 500..599
            }

            else -> false
        }
    }
}