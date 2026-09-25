package com.bober.autcsv.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.bober.autcsv.R
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
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
    private val db: com.bober.autcsv.data.local.ResumeDatabase,
    private val dao: ResumeDao,
    private val coverLetterDao: com.bober.autcsv.data.local.dao.CoverLetterDao,
    private val pdfStyleStore: com.bober.autcsv.core.pdf.PdfStyleStore,
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
            LlmLogger.logDatabaseQuery(
                "INSERT/UPDATE resume",
                mapOf("id" to resume.id, "name" to resume.personalInfo.fullName)
            )
            dao.insertResume(ResumeEntity.fromDomainModel(resume))
            LlmLogger.logDatabaseOperation("INSERT/UPDATE", "resume", resume.id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось сохранить резюме ${resume.id}", e)
            throw e
        }
    }

    /** Перемещает резюме в корзину (мягкое удаление). */
    override suspend fun deleteResume(id: String) = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("MOVE TO TRASH resume", mapOf("id" to id))
            dao.moveToTrash(id, System.currentTimeMillis())
            LlmLogger.logDatabaseOperation("MOVE TO TRASH", "resume", id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось переместить резюме $id в корзину", e)
            throw e
        }
    }

    /** Поток резюме из корзины. */
    override suspend fun getDeletedResumes(): Flow<List<Resume>> = withContext(Dispatchers.IO) {
        LlmLogger.logDatabaseQuery("SELECT deleted resumes", null)
        dao.getDeletedResumes()
            .map { entities ->
                LlmLogger.logDatabaseOperation("SELECT TRASH", "resume", "Count: ${entities.size}")
                entities.map { it.toDomainModel() }
            }
    }

    /** Восстанавливает резюме из корзины. */
    override suspend fun restoreResume(id: String) = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("RESTORE resume", mapOf("id" to id))
            dao.restoreResume(id)
            LlmLogger.logDatabaseOperation("RESTORE", "resume", id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось восстановить резюме $id", e)
            throw e
        }
    }

    /** Окончательно удаляет резюме вместе с письмами и файлом фото. */
    override suspend fun deleteResumePermanently(id: String) = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("DELETE resume permanently", mapOf("id" to id))
            // Каскад в одной транзакции: без неё сбой посреди удаления
            // оставлял бы письма-сироты и фото в filesDir навсегда
            db.withTransaction {
                deletePhotoFile(id)
                coverLetterDao.deleteLettersForResume(id)
                dao.deleteResume(id)
            }
            LlmLogger.logDatabaseOperation("DELETE", "resume", id)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось окончательно удалить резюме $id", e)
            throw e
        }
    }

    /** Окончательно очищает корзину вместе с письмами и файлами фото. */
    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logDatabaseQuery("EMPTY TRASH", null)
            db.withTransaction {
                dao.getTrashIds().forEach { id ->
                    deletePhotoFile(id)
                    coverLetterDao.deleteLettersForResume(id)
                }
                dao.emptyTrash()
            }
            LlmLogger.logDatabaseOperation("EMPTY TRASH", "resume", null)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось очистить корзину", e)
            throw e
        }
    }

    /** Удаляет файл фото резюме; трогаем только собственный каталог photos. */
    private fun deletePhotoFile(resumeId: String) {
        runCatching {
            val photo = File(context.filesDir, "photos/$resumeId.jpg")
            if (photo.exists()) photo.delete()
        }.onFailure {
            LlmLogger.logWarning("Не удалось удалить фото резюме $resumeId: ${it.message}")
        }
    }

    override suspend fun generatePdf(
        resume: Resume,
        templateType: PdfTemplateType,
        accentColor: Int?,
        sectionOrder: List<com.bober.autcsv.core.pdf.PdfSection>,
    ): String = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logUiEvent(
                "PDF",
                "Starting PDF generation",
                "Resume: ${resume.id}, Template: ${templateType.name}"
            )
            val startTime = System.currentTimeMillis()

            // Имя файла включает шаблон: мини-превью всех стилей генерируются
            // параллельно и не должны перезаписывать друг друга
            val pdfFile = File(context.cacheDir, "${resume.id}-${templateType.name}.pdf")

            val result = templateType
                .getTemplate(context, accentColor, sectionOrder, pdfStyleStore.embedResumeData)
                .generate(resume, pdfFile)

            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("PDF generation", duration)
            LlmLogger.logUiEvent(
                "PDF",
                "PDF generated successfully",
                "File: ${pdfFile.name}, Size: ${pdfFile.length()} bytes"
            )

            return@withContext result
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось сгенерировать PDF для резюме ${resume.id}", e)
            throw IOException("Failed to generate PDF: ${e.message}", e)
        }
    }

    override suspend fun exportDocx(resume: Resume): String = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "${resume.id}.docx")
        com.bober.autcsv.core.export.DocxExporter.export(context, resume, out)
    }

    override suspend fun exportCsv(resume: Resume?): String = withContext(Dispatchers.IO) {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val out =
            File(context.cacheDir, if (resume != null) "${resume.id}.csv" else "resumes-$stamp.csv")
        val content = if (resume != null) {
            com.bober.autcsv.core.export.CsvExporter.export(context, resume)
        } else {
            val all = dao.getAllResumesOnce()
            com.bober.autcsv.core.export.CsvExporter.exportAll(
                context,
                all.map { it.toDomainModel() })
        }
        out.writeText(content, Charsets.UTF_8)
        out.absolutePath
    }

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        // В бэкап уходит вся таблица, включая корзину: удалённые, но ещё
        // восстанавливаемые резюме не должны теряться при переносе
        val entities = dao.getAllResumesIncludingDeletedOnce()
        val resumes = entities.map { it.toDomainModel() }
        val gson = com.google.gson.Gson()

        // Фото встраиваем в base64: бэкап самодостаточен для переноса
        // между устройствами (файлы из filesDir в бэкап не попадают)
        val photos = mutableMapOf<String, String>()
        resumes.forEach { resume ->
            val path = resume.personalInfo.photoUri
            if (path.isNotBlank()) {
                val file = File(path)
                if (file.exists()) {
                    runCatching {
                        photos[resume.id] = android.util.Base64.encodeToString(
                            file.readBytes(),
                            android.util.Base64.NO_WRAP,
                        )
                    }
                }
            }
        }

        val payload = BackupPayload(
            app = "AutCSV",
            version = 1,
            exportedAt = System.currentTimeMillis(),
            resumes = resumes,
            photos = photos,
        )
        val json = gson.toJson(payload)

        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val name = "staffii-backup-$stamp.json"

        // Downloads через MediaStore: файл виден пользователю без прав
        com.bober.autcsv.core.utils.DownloadsSaver.save(
            context,
            name,
            "application/json"
        ) { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
        }
        LlmLogger.logDatabaseOperation("BACKUP", "resume", "Count: ${resumes.size}")
        name
    }

    override suspend fun importBackup(json: String): Int = withContext(Dispatchers.IO) {
        val gson = com.google.gson.Gson()
        val payload = runCatching { gson.fromJson(json, BackupPayload::class.java) }
            .getOrElse { throw IOException("Файл бэкапа повреждён: ${it.message}") }

        val resumes = payload.resumes
            ?: throw IOException("В бэкапе нет резюме")
        if (resumes.isEmpty()) throw IOException("В бэкапе нет резюме")

        // Транзакция: сбой посреди импорта не оставит полбазы;
        // REPLACE внутри неё по-прежнему затирает совпавшие id осознанно
        db.withTransaction {
            resumes.forEach { resume ->
                val photoBase64 = resume.id.let { payload.photos?.get(it) }
                val withPhoto = if (photoBase64 != null) {
                    val restored = restorePhotoFromBase64(resume.id, photoBase64)
                    if (restored != null) {
                        resume.copy(personalInfo = resume.personalInfo.copy(photoUri = restored))
                    } else resume
                } else resume
                dao.insertResume(ResumeEntity.fromDomainModel(withPhoto))
            }
        }
        LlmLogger.logDatabaseOperation("IMPORT", "resume", "Count: ${resumes.size}")
        resumes.size
    }

    /** Распаковывает фото из бэкапа в filesDir/photos/{id}.jpg; null при ошибке. */
    private fun restorePhotoFromBase64(resumeId: String, base64: String): String? {
        return runCatching {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val target = File(photosDir, "$resumeId.jpg")
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrNull()
    }

    override suspend fun analyzeResume(resume: Resume): Resume = withContext(Dispatchers.IO) {
        try {
            LlmLogger.logUiEvent("Analysis", "Starting resume analysis", "Resume: ${resume.id}")
            val startTime = System.currentTimeMillis()

            val analyzedResume = openRouterService.analyzeResume(resume)

            // Сохраняем результат, иначе окно анализа не увидит его (читает из БД)
            dao.insertResume(ResumeEntity.fromDomainModel(analyzedResume))

            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Resume analysis", duration)
            LlmLogger.logUiEvent("Analysis", "Resume analysis completed", "Resume: ${resume.id}")

            analyzedResume
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось проанализировать резюме ${resume.id}", e)
            throw IOException("Не удалось проанализировать резюме: ${e.message}", e)
        }
    }

    override suspend fun improveText(
        text: String,
        kind: com.bober.autcsv.data.api.llm.OpenRouterService.TextKind,
    ): String = withContext(Dispatchers.IO) {
        try {
            openRouterService.improveText(text, kind)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось улучшить текст", e)
            throw IOException("Не удалось улучшить текст: ${e.message}", e)
        }
    }

    override suspend fun generateAboutMe(resume: Resume): String = withContext(Dispatchers.IO) {
        try {
            openRouterService.generateAboutMe(resume)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось сгенерировать «О себе»", e)
            throw IOException("Не удалось сгенерировать «О себе»: ${e.message}", e)
        }
    }

    override suspend fun matchVacancy(
        resume: Resume,
        vacancyText: String,
    ): com.bober.autcsv.domain.model.VacancyMatch = withContext(Dispatchers.IO) {
        try {
            openRouterService.matchVacancy(resume, vacancyText)
        } catch (e: Exception) {
            LlmLogger.logError("Не удалось сопоставить резюме с вакансией", e)
            throw IOException("Не удалось сопоставить с вакансией: ${e.message}", e)
        }
    }

    override suspend fun duplicateResume(id: String): Resume? = withContext(Dispatchers.IO) {
        val source = dao.getResumeById(id)?.toDomainModel() ?: return@withContext null
        val copyId = java.util.UUID.randomUUID().toString()

        // Копируем файл фото, чтобы удаление оригинала не ломало копию
        val copiedPhoto = copyPhotoForDuplicate(source.personalInfo.photoUri, copyId)

        val copy = source.copy(
            id = copyId,
            personalInfo = source.personalInfo.copy(
                fullName = source.personalInfo.fullName + context.localizedString(R.string.duplicate_suffix),
                photoUri = copiedPhoto.orEmpty(),
            ),
            aiAnalysis = com.bober.autcsv.domain.model.AiAnalysis(),
            status = com.bober.autcsv.domain.model.CandidateStatus.NONE,
            isFavorite = false,
            lastModified = System.currentTimeMillis(),
        )
        dao.insertResume(ResumeEntity.fromDomainModel(copy))
        LlmLogger.logDatabaseOperation("DUPLICATE", "resume", copyId)
        copy
    }

    override suspend fun setStatus(
        id: String,
        status: com.bober.autcsv.domain.model.CandidateStatus,
    ) = withContext(Dispatchers.IO) {
        dao.getResumeById(id)?.let { entity ->
            dao.insertResume(
                entity.copy(
                    status = status.name,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        Unit
    }

    override suspend fun toggleFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        var newValue = false
        dao.getResumeById(id)?.let { entity ->
            newValue = !entity.isFavorite
            dao.insertResume(
                entity.copy(
                    isFavorite = newValue,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        newValue
    }

    // ── Сопроводительные письма ─────────────────────────────────────────

    override fun coverLettersFor(
        resumeId: String,
    ): Flow<List<com.bober.autcsv.domain.model.CoverLetter>> =
        coverLetterDao.lettersForResume(resumeId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun saveCoverLetter(letter: com.bober.autcsv.domain.model.CoverLetter) =
        withContext(Dispatchers.IO) {
            coverLetterDao.insertLetter(
                com.bober.autcsv.data.local.entity.CoverLetterEntity.fromDomainModel(letter)
            )
        }

    override suspend fun deleteCoverLetter(id: String) = withContext(Dispatchers.IO) {
        coverLetterDao.deleteLetter(id)
    }

    override suspend fun generateCoverLetter(
        resume: Resume,
        vacancyText: String,
    ): String = openRouterService.generateCoverLetter(resume, vacancyText)

    override suspend fun translateResume(
        resume: Resume,
        targetLanguageCode: String,
    ): Resume = withContext(Dispatchers.IO) {
        openRouterService.translateResume(resume, targetLanguageCode)
    }

    /** Копирует файл фото для дубликата; возвращает путь к копии или null. */
    private fun copyPhotoForDuplicate(photoUri: String, newResumeId: String): String? {
        if (photoUri.isBlank()) return null
        return try {
            val source = java.io.File(photoUri)
            if (!source.exists()) return null
            val photosDir = java.io.File(context.filesDir, "photos").apply { mkdirs() }
            val target = java.io.File(photosDir, "$newResumeId.jpg")
            source.copyTo(target, overwrite = true)
            target.absolutePath
        } catch (e: Exception) {
            LlmLogger.logWarning("Не удалось скопировать фото при дублировании: ${e.message}")
            null
        }
    }
}

/**
 * Формат JSON-бэкапа: список резюме + фото в base64. Поле app/version —
 * на случай развития формата; photos — map по id резюме.
 */
data class BackupPayload(
    val app: String = "AutCSV",
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val resumes: List<Resume>? = null,
    val photos: Map<String, String>? = null,
)