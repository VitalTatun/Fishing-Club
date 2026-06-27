package com.example.fishing.data.local

import android.content.Context
import androidx.room.Room
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fishing_database"
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideMarkerDao(database: AppDatabase): MarkerDao {
        return database.markerDao()
    }

    @Provides
    fun provideReportDetailsDao(database: AppDatabase): ReportDetailsDao {
        return database.reportDetailsDao()
    }
}