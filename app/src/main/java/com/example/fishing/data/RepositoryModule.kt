package com.example.fishing.data

import com.example.fishing.data.report.FavoritesStore
import com.example.fishing.data.report.HomeFeedStore
import com.example.fishing.data.report.LikesStore
import com.example.fishing.data.report.MarkerStore
import com.example.fishing.data.report.PhotoUrlProvider
import com.example.fishing.data.report.ReportDetailsStore
import com.example.fishing.data.report.ReportRemoteQueries
import com.example.fishing.data.report.ReportWriter
import com.example.fishing.data.report.RoomHomeFeedStore
import com.example.fishing.data.report.RoomMarkerStore
import com.example.fishing.data.report.RoomReportDetailsStore
import com.example.fishing.data.report.SupabaseFavoritesStore
import com.example.fishing.data.report.SupabaseLikesStore
import com.example.fishing.data.report.SupabasePhotoUrlProvider
import com.example.fishing.data.report.SupabaseReportWriter
import com.example.fishing.data.report.SupabaseReportRemoteQueries
import com.example.fishing.data.report.SupabaseUserReportsStore
import com.example.fishing.data.report.UserReportsStore
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

    @Binds
    @Singleton
    abstract fun bindReportRemoteQueries(
        queries: SupabaseReportRemoteQueries
    ): ReportRemoteQueries

    @Binds
    @Singleton
    abstract fun bindLikesStore(
        store: SupabaseLikesStore
    ): LikesStore

    @Binds
    @Singleton
    abstract fun bindFavoritesStore(
        store: SupabaseFavoritesStore
    ): FavoritesStore

    @Binds
    @Singleton
    abstract fun bindHomeFeedStore(
        store: RoomHomeFeedStore
    ): HomeFeedStore

    @Binds
    @Singleton
    abstract fun bindReportDetailsStore(
        store: RoomReportDetailsStore
    ): ReportDetailsStore

    @Binds
    @Singleton
    abstract fun bindMarkerStore(
        store: RoomMarkerStore
    ): MarkerStore

    @Binds
    @Singleton
    abstract fun bindReportWriter(
        writer: SupabaseReportWriter
    ): ReportWriter

    @Binds
    @Singleton
    abstract fun bindUserReportsStore(
        store: SupabaseUserReportsStore
    ): UserReportsStore
}
