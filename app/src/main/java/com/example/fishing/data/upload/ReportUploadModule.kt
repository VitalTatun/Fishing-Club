package com.example.fishing.data.upload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportUploadModule {

    @Binds
    @Singleton
    abstract fun bindReportUploadQueue(impl: RoomReportUploadQueue): ReportUploadQueue

    @Binds
    @Singleton
    abstract fun bindReportUploadScheduler(
        impl: WorkManagerReportUploadScheduler
    ): ReportUploadScheduler
}
