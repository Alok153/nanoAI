package com.vjaykrsna.nanoai.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for checking device connectivity state.
 */
interface ConnectivityStatusProvider {
    /** Returns true when the device currently has validated internet connectivity. */
    suspend fun isOnline(): Boolean
}

/**
 * Android implementation backed by [ConnectivityManager].
 */
@Singleton
class AndroidConnectivityStatusProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConnectivityStatusProvider {
        override suspend fun isOnline(): Boolean =
            withContext(Dispatchers.IO) {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork ?: return@withContext false
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return@withContext false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                            !requiresValidatedCapability()
                    )
            }

        @SuppressLint("ObsoleteSdkInt")
        private fun requiresValidatedCapability(): Boolean {
            // NET_CAPABILITY_VALIDATED reported starting from API 23. Fall back gracefully on older SDKs.
            return true
        }
    }
