package ru.purebytestudio.eventparser.data.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.purebytestudio.eventparser.platform.NetworkStatusProvider

/**
 * Android-реализация [NetworkStatusProvider] на базе [ConnectivityManager].
 *
 * `isOnline()` использует комбинацию:
 * - `NET_CAPABILITY_INTERNET` (есть доступ к интернету),
 * - `NET_CAPABILITY_VALIDATED` (интернет реально работает, а не только «подключены к Wi‑Fi»).
 *
 * `observe()` отдаёт поток, основанный на `NetworkCallback`, и всегда начинает со стартового значения.
 */
class AndroidNetworkStatusProvider(context: Context) : NetworkStatusProvider {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean = connectivityManager.run {
        val network = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun observe(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        trySend(isOnline())
        connectivityManager.registerNetworkCallback(
            request,
            callback
        )

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}