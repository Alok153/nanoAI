package com.vjaykrsna.nanoai.feature.library.data.huggingface.network

import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.HuggingFaceDeviceCodeResponse
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.HuggingFaceTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface HuggingFaceOAuthService {
  @FormUrlEncoded
  @POST("oauth/device/code")
  suspend fun requestDeviceCode(
    @Field("client_id") clientId: String,
    @Field("scope") scope: String,
  ): HuggingFaceDeviceCodeResponse

  @FormUrlEncoded
  @POST("oauth/token")
  suspend fun exchangeDeviceCode(
    @Field("client_id") clientId: String,
    @Field("device_code") deviceCode: String,
    @Field("grant_type") grantType: String = DEVICE_CODE_GRANT_TYPE,
  ): HuggingFaceTokenResponse

  companion object {
    private const val DEVICE_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
  }
}
