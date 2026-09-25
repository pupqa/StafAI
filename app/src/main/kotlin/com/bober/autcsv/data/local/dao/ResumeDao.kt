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
     * Поток активных (не удалённых) резюме. Подходит для отображения списка и автообновлений UI.
     */
    @Query("SELECT * FROM resumes WHERE isDeleted = 0")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    /**
     * Разовое чтение всех активных резюме (без Flow): для экспорта CSV.
     */
    @Query("SELECT * FROM resumes WHERE isDeleted = 0")
    suspend fun getAllResumesOnce(): List<ResumeEntity>

    /**
     * Разовое чтение всей таблицы, включая корзину: бэкап должен быть полным,
     * иначе удалённые, но восстанавливаемые резюме теряются при переносе.
     */
    @Query("SELECT * FROM resumes")
    suspend fun getAllResumesIncludingDeletedOnce(): List<ResumeEntity>

    /**
     * Идентификаторы резюме в корзине — для каскадной очистки писем/фото.
     */
    @Query("SELECT id FROM resumes WHERE isDeleted = 1")
    suspend fun getTrashIds(): List<String>

    /**
     * Поток резюме в корзине — удалённых мягко, но ещё не стёртых окончательно.
     */
    @Query("SELECT * FROM resumes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedResumes(): Flow<List<ResumeEntity>>

    /**
     * Мягкое удаление: переносит резюме в корзину с отметкой времени.
     */
    @Query("UPDATE resumes SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long)

    /**
     * Восстанавливает резюме из корзины, возвращая его в общий список.
     */
    @Query("UPDATE resumes SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    suspend fun restoreResume(id: String)

    /**
     * Окончательно удаляет резюме по идентификатору.
     */
    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResume(id: String)

    /**
     * Окончательно очищает корзину.
     */
    @Query("DELETE FROM resumes WHERE isDeleted = 1")
    suspend fun emptyTrash()
}
