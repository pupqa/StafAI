package com.bober.autcsv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bober.autcsv.data.local.converter.Converters
import com.bober.autcsv.data.local.dao.ResumeDao
import com.bober.autcsv.data.local.entity.ResumeEntity

@Database(
    entities = [ResumeEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ResumeDatabase : RoomDatabase() {
    abstract val resumeDao: ResumeDao

    companion object {
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