package com.vjaykrsna.nanoai.feature.library.data.huggingface.network

import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.HuggingFaceUserDto
import retrofit2.http.GET

/** Retrofit interface exposing Hugging Face account endpoints. */
interface HuggingFaceAccountService {
  /** Retrieve metadata for the currently authenticated Hugging Face user. */
  @GET("api/whoami-v2") suspend fun getCurrentUser(): HuggingFaceUserDto
}
