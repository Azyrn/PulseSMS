package com.skeler.pulse.sms

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

suspend fun ConnectivityManager.awaitNetwork(
    transport: Int,
    capability: Int,
    timeoutMs: Long = 5000,
): Network? = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        val request = NetworkRequest.Builder()
            .addTransportType(transport)
            .addCapability(capability)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (cont.isActive) cont.resume(network)
            }

            @Suppress("DEPRECATION")
            override fun onUnavailable() {
                if (cont.isActive) cont.resume(null)
            }
        }

        val handler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= 31) {
            requestNetwork(request, callback, handler, timeoutMs.toInt())
        } else {
            requestNetwork(request, callback, handler)
        }

        cont.invokeOnCancellation {
            unregisterNetworkCallback(callback)
        }
    }
}
