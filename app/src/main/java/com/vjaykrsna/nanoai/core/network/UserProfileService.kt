package com.vjaykrsna.nanoai.core.network

import com.vjaykrsna.nanoai.core.network.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Retrofit service interface for user profile operations.
 *
 * Defines endpoints for fetching and updating user UI/UX preferences and metadata.
 */
interface UserProfileService {
  /**
   * Fetch user profile with UI preferences and saved layouts.
   *
   * @return UserProfileDto containing user's UI/UX settings
   */
  @GET("user/profile") suspend fun getUserProfile(): UserProfileDto

  /**
   * Update user profile with new preferences.
   *
   * @param profile Updated user profile data
   * @return Updated UserProfileDto from server
   */
  @PUT("user/profile") suspend fun updateUserProfile(@Body profile: UserProfileDto): UserProfileDto
}
