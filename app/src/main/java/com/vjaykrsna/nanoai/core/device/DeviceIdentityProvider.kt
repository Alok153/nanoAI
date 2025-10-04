package com.vjaykrsna.nanoai.core.device

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Provides a stable, privacy-preserving identifier for manifest verification calls. */
interface DeviceIdentityProvider {
  /** Returns a hashed identifier unique to the current app installation. */
  fun deviceId(): String
}

@Singleton
class AndroidDeviceIdentityProvider
@Inject
constructor(
  @ApplicationContext private val context: Context,
) : DeviceIdentityProvider {
  override fun deviceId(): String {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val seed =
      if (androidId.isNullOrBlank()) {
        UUID.nameUUIDFromBytes(context.packageName.toByteArray()).toString()
      } else {
        "$androidId:${context.packageName}"
      }

    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(seed.toByteArray())
    return digest.digest().joinToString(separator = "") { byte ->
      String.format(Locale.US, "%02x", byte)
    }
  }
}
