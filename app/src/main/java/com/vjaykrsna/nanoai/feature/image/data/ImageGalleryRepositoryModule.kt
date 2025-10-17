package com.vjaykrsna.nanoai.feature.image.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ImageGalleryRepositoryModule {

  @Binds
  @Singleton
  fun bindImageGalleryRepository(impl: ImageGalleryRepositoryImpl): ImageGalleryRepository
}
