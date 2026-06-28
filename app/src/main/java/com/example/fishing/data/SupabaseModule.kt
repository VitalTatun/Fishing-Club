package com.example.fishing.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import java.io.File
import javax.inject.Singleton

internal object SupabaseConfig {
    const val URL = "https://newwheannhymapusagnm.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ld3doZWFubmh5bWFwdXNhZ25tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI4NDY0MzUsImV4cCI6MjA4ODQyMjQzNX0.qtW3zeoEfZmLvmiXlpDgQAw7CF12hWbiX8iTeqD1zNU"
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 10L * 1024 * 1024)

        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            httpEngine = OkHttp.create {
                config {
                    cache(cache)
                }
            }
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
