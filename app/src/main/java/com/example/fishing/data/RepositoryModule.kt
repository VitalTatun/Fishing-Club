package com.example.fishing.data

import com.example.fishing.data.report.PhotoUrlProvider
import com.example.fishing.data.report.SupabasePhotoUrlProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFishingRepository(
        repository: SupabaseFishingRepository
    ): FishingRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesSource(
        repository: UserPreferencesRepository
    ): UserPreferencesSource

    @Binds
    @Singleton
    abstract fun bindReportRemoteDataSource(
        dataSource: SupabaseReportRemoteDataSource
    ): ReportRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPhotoUrlProvider(
        provider: SupabasePhotoUrlProvider
    ): PhotoUrlProvider
}
