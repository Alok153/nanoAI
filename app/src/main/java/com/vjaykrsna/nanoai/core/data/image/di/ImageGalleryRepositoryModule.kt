package com.vjaykrsna.nanoai.core.data.image.di

import com.vjaykrsna.nanoai.core.data.image.ImageGalleryRepository
import com.vjaykrsna.nanoai.core.data.image.ImageGalleryRepositoryImpl
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
