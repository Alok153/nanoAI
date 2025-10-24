package com.vjaykrsna.nanoai.feature.uiux.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConnectivityRepositoryImplTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var repository: ConnectivityRepositoryImpl

  @BeforeEach
  fun setUp() {
    userProfileRepository = mockk(relaxed = true)
    coEvery { userProfileRepository.observePreferences() } returns flowOf(UiPreferencesSnapshot())

    repository =
      ConnectivityRepositoryImpl(
        userProfileRepository = userProfileRepository,
        ioDispatcher = mainDispatcherExtension.dispatcher,
      )
  }

  @Test
  fun `updateConnectivity should update the connectivity status`() = runTest {
    // Given
    val status = ConnectivityStatus.OFFLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.status).isEqualTo(status)
    coVerify { userProfileRepository.setOfflineOverride(true) }
  }

  @Test
  fun `connectivityBannerState should show CTA when offline`() = runTest {
    // Given
    val status = ConnectivityStatus.OFFLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.cta).isNotNull()
  }

  @Test
  fun `connectivityBannerState should not show CTA when online`() = runTest {
    // Given
    val status = ConnectivityStatus.ONLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.cta).isNull()
  }

  @Test
  fun `repository should implement ConnectivityRepository interface`() {
    // Then
    assertThat(repository).isInstanceOf(ConnectivityRepository::class.java)
  }

  @Test
  fun `repository should be properly constructed`() {
    // Verify that the repository can be constructed with dependencies
    assertThat(repository).isNotNull()
  }
}
