package com.bober.autcsv.domain.repository

import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.data.repository.Result
import com.bober.autcsv.domain.model.Resume
import kotlinx.coroutines.flow.Flow

interface ResumeRepository {
    /**
     * Возвращает резюме по идентификатору, оборачивая результат в [Result]
     * для единообразной обработки ошибок слоем выше.
     */
    suspend fun getResume(id: String): Result<Resume?>

    /**
     * Возвращает поток всех резюме в доменной модели. Используется для списков и автообновления UI.
     */
    suspend fun getAllResumes(): Flow<List<Resume>>

    /**
     * Сохраняет или обновляет резюме.
     */
    suspend fun saveResume(resume: Resume)

    /**
     * Удаляет резюме по идентификатору.
     */
    suspend fun deleteResume(id: String)

    /**
     * Генерирует PDF-файл для резюме с использованием выбранного шаблона.
     * Возвращает путь к сгенерированному файлу.
     */
    suspend fun generatePdf(
        resume: Resume,
        templateType: PdfTemplateType = PdfTemplateType.getDefault(),
    ): String

    /**
     * Запускает анализ резюме через LLM-сервис и возвращает обновлённую модель.
     */
    suspend fun analyzeResume(resume: Resume): Resume
} 