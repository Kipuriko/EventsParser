package ru.purebytestudio.eventparser.data.platform

import android.content.Context
import retrofit2.HttpException
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.platform.ErrorMessageProvider
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AndroidErrorMessageProvider(private val context: Context) : ErrorMessageProvider {
    override fun noInternet(): String = context.getString(R.string.error_no_internet)

    override fun fromThrowable(
        throwable: Throwable,
        fallback: String?
    ): String {
        return when (throwable) {
            is IOException -> mapIoException(throwable)
            is HttpException -> mapHttpException(throwable)
            else -> fallback ?: throwable.message ?: context.getString(R.string.error_unknown)
        }
    }

    private fun mapIoException(exception: IOException): String {
        return when (exception) {
            is SocketTimeoutException -> context.getString(R.string.error_timeout_retry)

            is UnknownHostException -> context.getString(R.string.error_connection)

            else -> {
                val message = exception.message?.lowercase().orEmpty()
                when {
                    "timeout" in message -> context.getString(R.string.error_timeout)
                    "connection" in message -> context.getString(R.string.error_connection_check)
                    else -> context.getString(R.string.error_connection)
                }
            }
        }
    }

    private fun mapHttpException(exception: HttpException): String {
        return when (exception.code()) {
            400 -> context.getString(R.string.error_bad_request)
            401 -> context.getString(R.string.error_unauthorized)
            403 -> context.getString(R.string.error_forbidden)
            404 -> context.getString(R.string.error_not_found)
            408 -> context.getString(R.string.error_request_timeout)
            429 -> context.getString(R.string.error_too_many_requests)
            in 500..599 -> context.getString(R.string.error_server)
            else -> context.getString(
                R.string.error_server_code,
                exception.code()
            )
        }
    }
}