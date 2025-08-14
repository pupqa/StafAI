package com.bober.autcsv.data.local.dao

import androidx.room.*
import com.bober.autcsv.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity)

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getResumeById(id: String): ResumeEntity?

    @Query("SELECT * FROM resumes")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResume(id: String)
} 