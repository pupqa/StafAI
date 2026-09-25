package com.bober.autcsv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bober.autcsv.data.local.converter.Converters
import com.bober.autcsv.data.local.dao.CoverLetterDao
import com.bober.autcsv.data.local.dao.ResumeDao
import com.bober.autcsv.data.local.entity.CoverLetterEntity
import com.bober.autcsv.data.local.entity.ResumeEntity

@Database(
    entities = [ResumeEntity::class, CoverLetterEntity::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ResumeDatabase : RoomDatabase() {
    abstract val resumeDao: ResumeDao
    abstract val coverLetterDao: CoverLetterDao

    companion object {
        /**
         * Миграция с 8 на 9: отдельная таблица сопроводительных писем
         * (LLM-генерация по вакансии, редактирование пользователем).
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cover_letters (
                        id TEXT PRIMARY KEY NOT NULL,
                        resumeId TEXT NOT NULL,
                        vacancyTitle TEXT NOT NULL,
                        vacancyText TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cover_letters_resumeId " +
                            "ON cover_letters(resumeId)"
                )
            }
        }

        /**
         * Миграция с 7 на 8: несколько образований у одного резюме.
         * Поле education сохраняется как текстовое представление для экспорта.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN educations TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        /**
         * Миграция с 6 на 7: желаемая занятость и график работы, фото
         * кандидата, статус в пайплайне рекрутера и признак избранного.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN employment TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN workSchedule TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN status TEXT NOT NULL DEFAULT 'NONE'"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Миграция с 5 на 6: блок «Желаемая работа» — зарплатная вилка,
         * готовность к релокации и города переезда, а также ссылки на соцсети.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN salaryMin TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN salaryMax TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN readyToRelocate TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN relocationCities TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN socialLinks TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        /**
         * Миграция с 4 на 5: добавление признака мягкого удаления (корзина)
         * и отметки времени удаления.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE resumes ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Миграция с 3 на 4: добавление поля aboutMe и переименование achievements
         * в professionalAchievements с переносом данных.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем поле aboutMe
                database.execSQL("ALTER TABLE resumes ADD COLUMN aboutMe TEXT NOT NULL DEFAULT ''")

                // Переименовываем achievements в professionalAchievements
                database.execSQL("ALTER TABLE resumes ADD COLUMN professionalAchievements TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("UPDATE resumes SET professionalAchievements = achievements")
                database.execSQL("ALTER TABLE resumes DROP COLUMN achievements")
            }
        }

        /**
         * Миграция с 2 на 3: детализируем поле технологий на несколько колонок,
         * переносим значения и удаляем старое поле technologies.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем новые поля для детализации технологий
                database.execSQL("ALTER TABLE resumes ADD COLUMN programmingLanguages TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE resumes ADD COLUMN frameworks TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE resumes ADD COLUMN libraries TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE resumes ADD COLUMN databases TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE resumes ADD COLUMN otherTechnologies TEXT NOT NULL DEFAULT '[]'")

                // Переносим существующие технологии в otherTechnologies
                database.execSQL("UPDATE resumes SET otherTechnologies = technologies")

                // Удаляем старое поле technologies
                database.execSQL("ALTER TABLE resumes DROP COLUMN technologies")
            }
        }

        /**
         * Миграция с 1 на 2: пересоздаем таблицу с расширенной схемой и переносим данные
         * через временную таблицу, затем удаляем временную.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Сначала создаем временную таблицу
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS resumes_temp (
                        id TEXT PRIMARY KEY NOT NULL,
                        fullName TEXT NOT NULL,
                        specialization TEXT NOT NULL,
                        totalExperience TEXT NOT NULL,
                        specializationExperience TEXT NOT NULL,
                        education TEXT NOT NULL,
                        email TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        languages TEXT NOT NULL,
                        operatingSystems TEXT NOT NULL,
                        technologies TEXT NOT NULL,
                        achievements TEXT NOT NULL
                    )
                """.trimIndent()
                )

                // Копируем существующие данные
                database.execSQL(
                    """
                    INSERT INTO resumes_temp (
                        id, fullName, specialization, totalExperience,
                        specializationExperience, education, email, phone,
                        languages, operatingSystems, technologies, achievements
                    )
                    SELECT 
                        id, fullName, specialization, totalExperience,
                        specializationExperience, education, email, phone,
                        languages, operatingSystems, technologies, achievements
                    FROM resumes
                """.trimIndent()
                )

                // Удаляем старую таблицу
                database.execSQL("DROP TABLE resumes")

                // Создаем новую таблицу с полной схемой
                database.execSQL(
                    """
                    CREATE TABLE resumes (
                        id TEXT PRIMARY KEY NOT NULL,
                        fullName TEXT NOT NULL,
                        specialization TEXT NOT NULL,
                        totalExperience TEXT NOT NULL,
                        specializationExperience TEXT NOT NULL,
                        education TEXT NOT NULL,
                        email TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        location TEXT NOT NULL DEFAULT '',
                        languages TEXT NOT NULL,
                        operatingSystems TEXT NOT NULL,
                        technologies TEXT NOT NULL,
                        achievements TEXT NOT NULL,
                        certifications TEXT NOT NULL DEFAULT '[]',
                        softSkills TEXT NOT NULL DEFAULT '[]',
                        projects TEXT NOT NULL DEFAULT '[]',
                        summary TEXT NOT NULL DEFAULT '',
                        lastModified INTEGER NOT NULL DEFAULT 0,
                        aiAnalysis TEXT NOT NULL DEFAULT '{}'
                    )
                """.trimIndent()
                )

                // Копируем данные из временной таблицы в новую
                database.execSQL(
                    """
                    INSERT INTO resumes (
                        id, fullName, specialization, totalExperience,
                        specializationExperience, education, email, phone,
                        languages, operatingSystems, technologies, achievements
                    )
                    SELECT 
                        id, fullName, specialization, totalExperience,
                        specializationExperience, education, email, phone,
                        languages, operatingSystems, technologies, achievements
                    FROM resumes_temp
                """.trimIndent()
                )

                // Удаляем временную таблицу
                database.execSQL("DROP TABLE resumes_temp")
            }
        }

        val callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
            }
        }
    }
} 