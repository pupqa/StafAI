package com.bober.autcsv.domain.repository

import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.data.repository.Result
import com.bober.autcsv.domain.model.Resume
import kotlinx.coroutines.flow.Flow

interface ResumeRepository {
    suspend fun getResume(id: String): Result<Resume?>
    suspend fun getAllResumes(): Flow<List<Resume>>
    suspend fun saveResume(resume: Resume)
    suspend fun deleteResume(id: String)
    suspend fun generatePdf(
        resume: Resume,
        templateType: PdfTemplateType = PdfTemplateType.getDefault(),
    ): String

    suspend fun analyzeResume(resume: Resume): Resume
} 