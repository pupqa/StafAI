package com.bober.autcsv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bober.autcsv.data.local.entity.CoverLetterEntity
import kotlinx.coroutines.flow.Flow

/** Доступ к сопроводительным письмам. */
@Dao
interface CoverLetterDao {

    /** Письма резюме, новые сверху. */
    @Query("SELECT * FROM cover_letters WHERE resumeId = :resumeId ORDER BY updatedAt DESC")
    fun lettersForResume(resumeId: String): Flow<List<CoverLetterEntity>>

    @Query("SELECT * FROM cover_letters WHERE id = :id")
    suspend fun getLetterById(id: String): CoverLetterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetter(letter: CoverLetterEntity)

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun deleteLetter(id: String)

    @Query("DELETE FROM cover_letters WHERE resumeId = :resumeId")
    suspend fun deleteLettersForResume(resumeId: String)
}
