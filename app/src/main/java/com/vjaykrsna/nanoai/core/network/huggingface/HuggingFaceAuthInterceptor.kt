package com.vjaykrsna.nanoai.core.network.huggingface

import com.vjaykrsna.nanoai.security.HuggingFaceTokenProvider
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/** Adds Hugging Face bearer authentication headers to outbound requests when available. */
@Singleton
class HuggingFaceAuthInterceptor
@Inject
constructor(private val tokenProvider: HuggingFaceTokenProvider) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val token = tokenProvider.accessToken()?.takeIf { it.isNotBlank() }
    val original = chain.request()
    val request =
      if (token != null) {
        original
          .newBuilder()
          .addHeader("Authorization", "Bearer $token")
          .addHeader("User-Agent", DEFAULT_USER_AGENT)
          .build()
      } else {
        original.newBuilder().addHeader("User-Agent", DEFAULT_USER_AGENT).build()
      }
    return chain.proceed(request)
  }

  companion object {
    private const val DEFAULT_USER_AGENT = "nanoAI/1.0 (Android)"
  }
}
