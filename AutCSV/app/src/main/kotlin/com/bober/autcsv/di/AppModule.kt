package com.bober.autcsv.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.bober.autcsv.data.api.llm.OpenRouterService
import com.bober.autcsv.data.local.ResumeDatabase
import com.bober.autcsv.data.local.dao.ResumeDao
import com.bober.autcsv.data.repository.ResumeRepositoryImpl
import com.bober.autcsv.domain.repository.ResumeRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideResumeDatabase(app: Application): ResumeDatabase {
        return Room.databaseBuilder(
            app,
            ResumeDatabase::class.java,
            "resume.db"
        )
            .addCallback(ResumeDatabase.callback)
            .addMigrations(ResumeDatabase.MIGRATION_1_2, ResumeDatabase.MIGRATION_2_3, ResumeDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideResumeDao(db: ResumeDatabase): ResumeDao = db.resumeDao

    @Provides
    @Singleton
    fun provideResumeRepository(
        dao: ResumeDao,
        openRouterService: OpenRouterService,
        @ApplicationContext context: Context,
    ): ResumeRepository {
        return ResumeRepositoryImpl(dao, openRouterService, context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
} 