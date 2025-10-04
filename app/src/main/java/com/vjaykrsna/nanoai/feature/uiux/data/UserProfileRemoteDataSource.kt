package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.network.UserProfileService
import com.vjaykrsna.nanoai.core.network.dto.toDomain
import com.vjaykrsna.nanoai.core.network.dto.toDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Remote data source for user profile operations via REST API.
 *
 * Wraps [UserProfileService] to handle API calls, error handling, and DTO conversion. Provides a
 * clean interface for repository implementations.
 */
@Singleton
class UserProfileRemoteDataSource
@Inject
constructor(
  private val userProfileService: UserProfileService,
) {
  /**
   * Fetch user profile from remote API.
   *
   * @return Domain UserProfile model
   * @throws Exception if network request fails or response is invalid
   */
  suspend fun fetchUserProfile(): Result<UserProfile> =
    runCatching { userProfileService.getUserProfile() }
      .map { it.toDomain() }
      .fold(
        onSuccess = { Result.success(it) },
        onFailure = {
          Result.failure(
            mapRemoteFailure(message = "Failed to fetch user profile", cause = it),
          )
        },
      )

  /**
   * Update user profile on remote server.
   *
   * @param profile Updated user profile
   * @return Updated domain UserProfile model from server response
   * @throws Exception if network request fails or response is invalid
   */
  suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> =
    runCatching {
        val dto = profile.toDto()
        userProfileService.updateUserProfile(dto)
      }
      .map { it.toDomain() }
      .fold(
        onSuccess = { Result.success(it) },
        onFailure = {
          Result.failure(
            mapRemoteFailure(message = "Failed to update user profile", cause = it),
          )
        },
      )

  private fun mapRemoteFailure(message: String, cause: Throwable): RemoteDataSourceException {
    val detailedMessage =
      when (cause) {
        is HttpException -> "$message (HTTP ${cause.code()})"
        is IOException -> "$message (network error)"
        is SerializationException -> "$message (invalid response)"
        else -> message
      }
    return RemoteDataSourceException(detailedMessage, cause)
  }
}

/**
 * Exception thrown when remote data source operations fail.
 *
 * Wraps underlying network or parsing errors with context about the operation.
 */
class RemoteDataSourceException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
