package com.bober.autcsv.data.repository

import android.content.Context
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.data.api.llm.OpenRouterService
import com.bober.autcsv.data.local.dao.ResumeDao
import com.bober.autcsv.data.local.entity.ResumeEntity
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class ResumeRepositoryImpl @Inject constructor(
    private val dao: ResumeDao,
    private val openRouterService: OpenRouterService,
    @ApplicationContext private val context: Context,
) : ResumeRepository {

    override suspend fun getResume(id: String): Result<Resume?> = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("SELECT resume by ID", mapOf("id" to id))
            val entity = dao.getResumeById(id)
            LlmLogger.logDatabaseOperation("SELECT", "resume", id)
            return@withContext Result.Success(entity?.toDomainModel())
        } catch (e: Exception) {
            LlmLogger.logError("Ошибка запроса к базе данных для резюме $id", e)
            return@withContext Result.Error(e)
        }
    }

    override suspend fun getAllResumes(): Flow<List<Resume>> = withContext(Dispatchers.IO) {
        LlmLogger.logDatabaseQuery("SELECT all resumes", null)
        dao.getAllResumes()
            .map { entities -> 
                LlmLogger.logDatabaseOperation("SELECT ALL", "resume", "Count: ${entities.size}")
                entities.map { it.toDomainModel() } 
            }
    }

    override suspend fun saveResume(resume: Resume) = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("INSERT/UPDATE resume", mapOf("id" to resume.id, "name" to resume.personalInfo.fullName))
            dao.insertResume(ResumeEntity.fromDomainModel(resume))
            LlmLogger.logDatabaseOperation("INSERT/UPDATE", "resume", resume.id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось сохранить резюме ${resume.id}", e)
            throw e
        }
    }

    override suspend fun deleteResume(id: String) = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("DELETE resume", mapOf("id" to id))
            dao.deleteResume(id)
            LlmLogger.logDatabaseOperation("DELETE", "resume", id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось удалить резюме $id", e)
            throw e
        }
    }

    override suspend fun generatePdf(resume: Resume, templateType: PdfTemplateType): String =
        withContext(Dispatchers.IO) {
            try {
                LlmLogger.logUiEvent("PDF", "Starting PDF generation", "Resume: ${resume.id}, Template: ${templateType.name}")
                val startTime = System.currentTimeMillis()
                
                // Create PDF file in app's cache directory
                val pdfFile = File(context.cacheDir, "${resume.id}.pdf")

                // Generate PDF using selected template
                val result = templateType.getTemplate().generate(resume, pdfFile)
                
                val duration = System.currentTimeMillis() - startTime
                LlmLogger.logPerformance("PDF generation", duration)
                LlmLogger.logUiEvent("PDF", "PDF generated successfully", "File: ${pdfFile.name}, Size: ${pdfFile.length()} bytes")
                
                return@withContext result
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось сгенерировать PDF для резюме ${resume.id}", e)
                throw IOException("Failed to generate PDF: ${e.message}", e)
            }
        }

    override suspend fun analyzeResume(resume: Resume): Resume = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logUiEvent("Analysis", "Starting resume analysis", "Resume: ${resume.id}")
            val startTime = System.currentTimeMillis()
            
            val result = openRouterService.analyzeResume(resume)
            
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Resume analysis", duration)
            LlmLogger.logUiEvent("Analysis", "Resume analysis completed", "Resume: ${resume.id}")
            
            return@withContext result
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось проанализировать резюме ${resume.id}", e)
            throw IOException("Не удалось проанализировать резюме: ${e.message}", e)
        }
    }
} 