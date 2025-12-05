package com.vjaykrsna.nanoai.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Abstraction for checking device connectivity state. */
interface ConnectivityStatusProvider {
  /** Returns true when the device currently has validated internet connectivity. */
  suspend fun isOnline(): Boolean
}

/** Android implementation backed by [ConnectivityManager]. */
@Singleton
class AndroidConnectivityStatusProvider
@Inject
constructor(@ApplicationContext private val context: Context) : ConnectivityStatusProvider {
  override suspend fun isOnline(): Boolean =
    withContext(Dispatchers.IO) {
      val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = connectivityManager.activeNetwork ?: return@withContext false
      val capabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return@withContext false
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
          !requiresValidatedCapability())
    }

  private fun requiresValidatedCapability(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}
