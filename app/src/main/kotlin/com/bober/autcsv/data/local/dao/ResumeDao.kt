package com.bober.autcsv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bober.autcsv.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    /**
     * Вставляет или обновляет резюме. При конфликте по первичному ключу запись заменяется.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity)

    /**
     * Возвращает резюме по идентификатору или null, если не найдено.
     */
    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getResumeById(id: String): ResumeEntity?

    /**
     * Поток всех резюме. Подходит для отображения списка и автообновлений UI.
     */
    @Query("SELECT * FROM resumes")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    /**
     * Удаляет резюме по идентификатору.
     */
    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResume(id: String)
} 