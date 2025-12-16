package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCaseInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ModelLibraryUseCaseModule {
  @Binds
  fun bindModelDownloadsAndExportUseCase(
    impl: ModelDownloadsAndExportUseCase
  ): ModelDownloadsAndExportUseCaseInterface
}
