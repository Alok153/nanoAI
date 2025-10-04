package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.network.UserProfileService
import com.vjaykrsna.nanoai.core.network.dto.toDomain
import com.vjaykrsna.nanoai.core.network.dto.toDto
import javax.inject.Inject
import javax.inject.Singleton

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
    try {
      val dto = userProfileService.getUserProfile()
      Result.success(dto.toDomain())
    } catch (e: Exception) {
      Result.failure(RemoteDataSourceException("Failed to fetch user profile", e))
    }

  /**
   * Update user profile on remote server.
   *
   * @param profile Updated user profile
   * @return Updated domain UserProfile model from server response
   * @throws Exception if network request fails or response is invalid
   */
  suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> =
    try {
      val dto = profile.toDto()
      val responseDto = userProfileService.updateUserProfile(dto)
      Result.success(responseDto.toDomain())
    } catch (e: Exception) {
      Result.failure(RemoteDataSourceException("Failed to update user profile", e))
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
