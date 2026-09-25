package com.bober.autcsv.domain.repository

import com.bober.autcsv.core.pdf.PdfSection
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.data.api.llm.OpenRouterService
import com.bober.autcsv.data.repository.Result
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.VacancyMatch
import kotlinx.coroutines.flow.Flow

interface ResumeRepository {
    /**
     * Возвращает резюме по идентификатору, оборачивая результат в [Result]
     * для единообразной обработки ошибок слоем выше.
     */
    suspend fun getResume(id: String): Result<Resume?>

    /**
     * Возвращает поток всех активных (не находящихся в корзине) резюме.
     */
    suspend fun getAllResumes(): Flow<List<Resume>>

    /**
     * Возвращает поток резюме, находящихся в корзине.
     */
    suspend fun getDeletedResumes(): Flow<List<Resume>>

    /**
     * Сохраняет или обновляет резюме.
     */
    suspend fun saveResume(resume: Resume)

    /**
     * Перемещает резюме в корзину (мягкое удаление с возможностью восстановления).
     */
    suspend fun deleteResume(id: String)

    /**
     * Восстанавливает резюме из корзины, возвращая его в общий список.
     */
    suspend fun restoreResume(id: String)

    /**
     * Окончательно удаляет резюме из корзины без возможности восстановления.
     */
    suspend fun deleteResumePermanently(id: String)

    /**
     * Окончательно очищает корзину.
     */
    suspend fun emptyTrash()

    /**
     * Генерирует PDF-файл для резюме с выбранным шаблоном, акцентным цветом
     * и порядком секций. Возвращает путь к сгенерированному файлу.
     */
    suspend fun generatePdf(
        resume: Resume,
        templateType: PdfTemplateType = PdfTemplateType.getDefault(),
        accentColor: Int? = null,
        sectionOrder: List<PdfSection> = PdfSection.defaultOrder(),
    ): String

    /**
     * Экспорт одного резюме в DOCX (редактируемый формат для рекрутеров).
     * Возвращает путь к файлу.
     */
    suspend fun exportDocx(resume: Resume): String

    /**
     * Экспорт одного или всех резюме в CSV. Возвращает путь к файлу.
     */
    suspend fun exportCsv(resume: Resume?): String

    /**
     * Локальный бэкап всей базы в JSON (перенос между устройствами, №33):
     * резюме + фото кандидатов, закодированные в base64. Возвращает путь
     * к файлу в общедоступных загрузках.
     */
    suspend fun exportBackup(): String

    /**
     * Восстановление из JSON-бэкапа (№34): сохраняет все резюме из файла,
     * возвращает количество импортированных записей.
     */
    suspend fun importBackup(json: String): Int

    /**
     * Запускает анализ резюме через LLM-сервис и возвращает обновлённую модель.
     */
    suspend fun analyzeResume(resume: Resume): Resume

    /**
     * Улучшает текст резюме через LLM (о себе, описания проектов, достижения).
     */
    suspend fun improveText(text: String, kind: OpenRouterService.TextKind): String

    /**
     * Генерирует раздел «О себе» на основе заполненных данных резюме.
     */
    suspend fun generateAboutMe(resume: Resume): String

    /**
     * Сопоставляет резюме с текстом вакансии: оценка, совпадения и пробелы.
     */
    suspend fun matchVacancy(resume: Resume, vacancyText: String): VacancyMatch

    /**
     * Дублирует резюме с новым идентификатором («копия» в имени, без
     * избранного/статуса); возвращает созданную копию.
     */
    suspend fun duplicateResume(id: String): Resume?

    /**
     * Переводит резюме в указанный статус пайплайна рекрутера.
     */
    suspend fun setStatus(id: String, status: com.bober.autcsv.domain.model.CandidateStatus)

    /**
     * Переключает признак избранного; возвращает новое значение.
     */
    suspend fun toggleFavorite(id: String): Boolean

    // ── Сопроводительные письма ─────────────────────────────────────────

    /** Поток писем резюме, новые сверху. */
    fun coverLettersFor(resumeId: String): Flow<List<com.bober.autcsv.domain.model.CoverLetter>>

    /** Сохраняет (создаёт или обновляет) письмо. */
    suspend fun saveCoverLetter(letter: com.bober.autcsv.domain.model.CoverLetter)

    /** Удаляет письмо окончательно. */
    suspend fun deleteCoverLetter(id: String)

    /**
     * Генерирует сопроводительное письмо по резюме и тексту вакансии
     * через LLM; возвращает текст письма на языке интерфейса.
     */
    suspend fun generateCoverLetter(resume: Resume, vacancyText: String): String

    /**
     * Переводит резюме на целевой язык («ru»/«en») через LLM:
     * свободные тексты переводятся, структура сохраняется.
     * Возвращает переведённую копию (без сохранения в БД).
     */
    suspend fun translateResume(resume: Resume, targetLanguageCode: String): Resume
}