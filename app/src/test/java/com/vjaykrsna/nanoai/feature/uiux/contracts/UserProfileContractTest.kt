package com.vjaykrsna.nanoai.feature.uiux.contracts

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

/**
 * Contract test for `/user/profile` endpoint based on `specs/003-UI-UX/contracts/openapi.yaml`.
 *
 * The test intentionally fails until the production client is implemented.
 */
@OptIn(ExperimentalSerializationApi::class)
class UserProfileContractTest {
  private lateinit var server: MockWebServer
  private lateinit var service: UserProfileService

  private val json = Json { ignoreUnknownKeys = false }

  @Before
  fun setUp() {
    server = MockWebServer().apply { start() }
    service =
      Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(UserProfileService::class.java)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `get user profile matches OpenAPI schema`() = runBlocking {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
                        {
                          "id": "user-123",
                          "displayName": "Taylor",
                          "themePreference": "LIGHT"
                        }
                    """
            .trimIndent(),
        ),
    )

    val response = service.getUserProfile()
    val body = requireNotNull(response.body()) { "Expected response body to be non-null" }

    assertThat(body.id).isEqualTo("user-123")
    assertThat(body.displayName).isEqualTo("Taylor")
    assertThat(body.themePreference).isEqualTo("LIGHT")

    assertRequestMatchesOpenApi(server.takeRequest())

    fail("T005: Implement real client wiring for /user/profile before marking task complete")
  }

  private fun assertRequestMatchesOpenApi(request: RecordedRequest) {
    assertThat(request.method).isEqualTo("GET")
    assertThat(request.path).isEqualTo("/user/profile")
  }

  @Serializable
  private data class UserProfileResponse(
    val id: String,
    val displayName: String,
    @SerialName("themePreference") val themePreference: String,
  )

  private interface UserProfileService {
    @GET("user/profile") suspend fun getUserProfile(): retrofit2.Response<UserProfileResponse>
  }
}
