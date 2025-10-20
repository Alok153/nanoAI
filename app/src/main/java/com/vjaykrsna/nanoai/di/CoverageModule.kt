package com.vjaykrsna.nanoai.di

import android.content.Context
import com.vjaykrsna.nanoai.coverage.data.CoverageDashboardRepository
import com.vjaykrsna.nanoai.coverage.domain.usecase.GetCoverageReportUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object CoverageModule {

  @Provides
  @Singleton
  fun provideCoverageDashboardRepository(
    @ApplicationContext context: Context,
    json: Json,
  ): CoverageDashboardRepository = CoverageDashboardRepository(context, json)

  @Provides
  @Singleton
  fun provideGetCoverageReportUseCase(
    repository: CoverageDashboardRepository
  ): GetCoverageReportUseCase = GetCoverageReportUseCase(repository)
}
