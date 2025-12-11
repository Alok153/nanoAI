package com.vjaykrsna.nanoai.feature.di

import com.vjaykrsna.nanoai.feature.audio.data.AudioFeatureDataSource
import com.vjaykrsna.nanoai.feature.audio.data.CoreAudioFeatureDataSource
import com.vjaykrsna.nanoai.feature.audio.data.DefaultAudioFeatureRepository
import com.vjaykrsna.nanoai.feature.audio.domain.AudioFeatureRepository
import com.vjaykrsna.nanoai.feature.image.data.CoreImageFeatureDataSource
import com.vjaykrsna.nanoai.feature.image.data.DefaultImageFeatureRepository
import com.vjaykrsna.nanoai.feature.image.data.ImageFeatureDataSource
import com.vjaykrsna.nanoai.feature.image.domain.ImageFeatureRepository
import com.vjaykrsna.nanoai.feature.library.data.CoreModelDownloadDataSource
import com.vjaykrsna.nanoai.feature.library.data.DefaultModelDownloadRepository
import com.vjaykrsna.nanoai.feature.library.data.ModelDownloadDataSource
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadRepository
import com.vjaykrsna.nanoai.feature.settings.data.BackupDataSource
import com.vjaykrsna.nanoai.feature.settings.data.CoreBackupDataSource
import com.vjaykrsna.nanoai.feature.settings.data.CorePersonaDataSource
import com.vjaykrsna.nanoai.feature.settings.data.DefaultBackupRepository
import com.vjaykrsna.nanoai.feature.settings.data.DefaultPersonaRepository
import com.vjaykrsna.nanoai.feature.settings.data.PersonaDataSource
import com.vjaykrsna.nanoai.feature.settings.domain.BackupRepository
import com.vjaykrsna.nanoai.feature.settings.domain.PersonaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for feature-level repositories and their data sources. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindAudioFeatureRepository(
    impl: DefaultAudioFeatureRepository
  ): AudioFeatureRepository

  @Binds
  @Singleton
  abstract fun bindAudioFeatureDataSource(impl: CoreAudioFeatureDataSource): AudioFeatureDataSource

  @Binds
  @Singleton
  abstract fun bindImageFeatureRepository(
    impl: DefaultImageFeatureRepository
  ): ImageFeatureRepository

  @Binds
  @Singleton
  abstract fun bindImageFeatureDataSource(impl: CoreImageFeatureDataSource): ImageFeatureDataSource

  @Binds
  @Singleton
  abstract fun bindModelDownloadRepository(
    impl: DefaultModelDownloadRepository
  ): ModelDownloadRepository

  @Binds
  @Singleton
  abstract fun bindModelDownloadDataSource(
    impl: CoreModelDownloadDataSource
  ): ModelDownloadDataSource

  @Binds
  @Singleton
  abstract fun bindPersonaRepository(impl: DefaultPersonaRepository): PersonaRepository

  @Binds
  @Singleton
  abstract fun bindPersonaDataSource(impl: CorePersonaDataSource): PersonaDataSource

  @Binds
  @Singleton
  abstract fun bindBackupRepository(impl: DefaultBackupRepository): BackupRepository

  @Binds @Singleton abstract fun bindBackupDataSource(impl: CoreBackupDataSource): BackupDataSource
}
