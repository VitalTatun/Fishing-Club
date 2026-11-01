package com.example.fishing.data.cleanup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrphanPhotoCleanupModule {

    @Binds
    @Singleton
    abstract fun bindOrphanPhotoCleaner(impl: SupabaseOrphanPhotoCleaner): OrphanPhotoCleaner

    @Binds
    @Singleton
    abstract fun bindOrphanPhotoCleanupScheduler(
        impl: WorkManagerOrphanPhotoCleanupScheduler
    ): OrphanPhotoCleanupScheduler
}
