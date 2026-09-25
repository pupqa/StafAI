package com.bober.autcsv.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.bober.autcsv.BuildConfig
import com.bober.autcsv.data.api.llm.OpenRouterService
import com.bober.autcsv.data.local.ResumeDatabase
import com.bober.autcsv.data.local.dao.CoverLetterDao
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
/**
 * Базовый DI-модуль приложения: БД, DAO, репозиторий, Gson.
 */
object AppModule {

    @Provides
    @Singleton
            /** Создаёт базу данных Room с миграциями и callback. */
    fun provideResumeDatabase(app: Application): ResumeDatabase {
        return Room.databaseBuilder(
            app,
            ResumeDatabase::class.java,
            "resume.db"
        )
            .addCallback(ResumeDatabase.callback)
            .addMigrations(
                ResumeDatabase.MIGRATION_1_2,
                ResumeDatabase.MIGRATION_2_3,
                ResumeDatabase.MIGRATION_3_4,
                ResumeDatabase.MIGRATION_4_5,
                ResumeDatabase.MIGRATION_5_6,
                ResumeDatabase.MIGRATION_6_7,
                ResumeDatabase.MIGRATION_7_8,
                ResumeDatabase.MIGRATION_8_9
            )
            // В debug забытая миграция просто пересоздаёт базу; в release
            // отсутствие миграции — падение, а не молчаливая потеря резюме
            .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration(false) }
            .build()
    }

    @Provides
    @Singleton
            /** Провайдер DAO резюме. */
    fun provideResumeDao(db: ResumeDatabase): ResumeDao = db.resumeDao

    @Provides
    @Singleton
            /** Провайдер DAO сопроводительных писем. */
    fun provideCoverLetterDao(db: ResumeDatabase): CoverLetterDao = db.coverLetterDao

    @Provides
    @Singleton
            /** Репозиторий резюме: БД + LLM сервис + контекст (для PDF). */
    fun provideResumeRepository(
        db: ResumeDatabase,
        dao: ResumeDao,
        coverLetterDao: CoverLetterDao,
        pdfStyleStore: com.bober.autcsv.core.pdf.PdfStyleStore,
        openRouterService: OpenRouterService,
        @ApplicationContext context: Context,
    ): ResumeRepository {
        return ResumeRepositoryImpl(
            db,
            dao,
            coverLetterDao,
            pdfStyleStore,
            openRouterService,
            context
        )
    }

    @Provides
    @Singleton
            /** Провайдер Gson для сериализации/десериализации. */
    fun provideGson(): Gson {
        return Gson()
    }
}
