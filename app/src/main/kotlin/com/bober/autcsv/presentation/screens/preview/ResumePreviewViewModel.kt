package com.bober.autcsv.presentation.screens.preview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.pdf.PdfSection
import com.bober.autcsv.core.pdf.PdfStyleStore
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Состояние экрана предпросмотра: резюме, прогрессы генерации/анализа,
 * стиль PDF (шаблон + акцент + порядок секций, №6/№18/№24), мини-превью
 * первой страницы каждого шаблона (№16), страницы документа для
 * постраничного просмотра и статусы экспорта.
 */
data class ResumePreviewState(
    val resume: Resume? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPdfGenerating: Boolean = false,
    val isPdfReady: Boolean = false,
    val pdfGenerationError: String? = null,
    val selectedTemplate: PdfTemplateType = PdfTemplateType.PROFESSIONAL,
    val accentColor: Int? = null,
    val sectionOrder: List<PdfSection> = PdfSection.defaultOrder(),
    /** Живые мини-превью первой страницы для каждого шаблона. */
    val thumbnails: Map<PdfTemplateType, ImageBitmap> = emptyMap(),
    /** Путь к актуальному PDF — нужен для печати и «поделиться». */
    val pdfPath: String? = null,
    /**
     * Страницы актуального PDF выбранного шаблона для основного предпросмотра;
     * null — битмап страницы ещё рендерится. Пустой список — рендер не начат
     * (в UI тогда показывается мини-превью первой страницы).
     */
    val previewPages: List<ImageBitmap?> = emptyList(),
    val isAnalyzing: Boolean = false,
    val analyzeError: String? = null,
    val aiSuggestionsApplied: Boolean = false,
    /** Идёт LLM-перевод резюме (кнопка «Перевести»). */
    val isTranslating: Boolean = false,
    /** Информационное сообщение о результате экспорта (snackbar-стиль). */
    val exportMessage: String? = null,
    val isExporting: Boolean = false,
    /** Id резюме для перехода на анализ; сбрасывается через [consumeAnalysisNavigation]. */
    val navigateToAnalysisResumeId: String? = null,
)

/**
 * ViewModel экрана предпросмотра: загружает резюме, рендерит мини-превью
 * всех шаблонов, хранит выбранный стиль в [PdfStyleStore], генерирует
 * PDF/DOCX/CSV и инициирует анализ через репозиторий.
 */
@HiltViewModel
class ResumePreviewViewModel @Inject constructor(
    private val repository: ResumeRepository,
    private val styleStore: PdfStyleStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ResumePreviewState(
            selectedTemplate = styleStore.templateType,
            accentColor = styleStore.accentColor,
            sectionOrder = styleStore.sectionOrder,
        )
    )
    val state: StateFlow<ResumePreviewState> = _state

    /** Обрабатывает события экрана и делегирует действия соответствующим методам. */
    fun onEvent(event: ResumePreviewEvent) {
        when (event) {
            is ResumePreviewEvent.LoadResume -> loadResume(event.id)
            is ResumePreviewEvent.SelectTemplate -> {
                styleStore.templateType = event.templateType
                _state.update { it.copy(selectedTemplate = event.templateType) }
                _state.value.resume?.let { generatePdf(it) }
            }

            is ResumePreviewEvent.SelectAccentColor -> {
                styleStore.accentColor = event.color
                _state.update { it.copy(accentColor = event.color) }
                _state.value.resume?.let {
                    generatePdf(it)
                    renderThumbnails(it)
                }
            }

            is ResumePreviewEvent.MoveSection -> moveSection(event.section, event.up)

            is ResumePreviewEvent.SharePdf -> sharePdf()
            is ResumePreviewEvent.DownloadPdf -> downloadPdf()
            is ResumePreviewEvent.PrintPdf -> Unit // печать выполняется из экрана с Activity-контекстом
            is ResumePreviewEvent.ExportDocx -> exportDocument(ExportKind.DOCX)
            is ResumePreviewEvent.ExportCsv -> exportDocument(ExportKind.CSV)
            is ResumePreviewEvent.AnalyzeResume -> analyzeResume()
            is ResumePreviewEvent.ApplyAiSuggestions -> applyAiSuggestions()
            is ResumePreviewEvent.ExportResume -> exportResume()
        }
    }

    /** Экран обработал навигацию к анализу — сбрасываем флаг. */
    fun consumeAnalysisNavigation() {
        _state.update { it.copy(navigateToAnalysisResumeId = null) }
    }

    /** Загружает резюме, рендерит превью шаблонов и генерирует PDF. */
    private fun loadResume(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = repository.getResume(id)) {
                    is com.bober.autcsv.data.repository.Result.Success -> {
                        _state.update { it.copy(resume = result.data, isLoading = false) }
                        result.data?.let { resume ->
                            renderThumbnails(resume)
                            generatePdf(resume)
                        }
                    }

                    is com.bober.autcsv.data.repository.Result.Error -> {
                        _state.update {
                            it.copy(
                                error = result.exception.message
                                    ?: context.localizedString(R.string.error_loading),
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: context.localizedString(R.string.error_loading),
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Перенос секции на позицию вверх/вниз с сохранением в store (№24). */
    private fun moveSection(section: PdfSection, up: Boolean) {
        val current = _state.value.sectionOrder.toMutableList()
        val index = current.indexOf(section)
        val target = if (up) index - 1 else index + 1
        if (index == -1 || target !in current.indices) return
        current[index] = current[target]
        current[target] = section
        val newOrder = current.toList()
        styleStore.sectionOrder = newOrder
        _state.update { it.copy(sectionOrder = newOrder) }
        _state.value.resume?.let {
            generatePdf(it)
            renderThumbnails(it)
        }
    }

    private fun generatePdf(resume: Resume) {
        viewModelScope.launch {
            _state.update { it.copy(isPdfGenerating = true, pdfGenerationError = null) }
            try {
                val s = _state.value
                val path = repository.generatePdf(
                    resume, s.selectedTemplate, s.accentColor, s.sectionOrder,
                )
                _state.update {
                    it.copy(
                        isPdfGenerating = false,
                        isPdfReady = true,
                        pdfPath = path,
                        pdfGenerationError = null
                    )
                }
                renderDocumentPages(File(path))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPdfGenerating = false,
                        isPdfReady = false,
                        pdfGenerationError = context.localizedString(
                            R.string.error_pdf_generate_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Рендерит первую страницу каждого шаблона в мини-превью (№16):
     * генерация PDF → PdfRenderer → Bitmap → ImageBitmap.
     *
     * Шаблоны генерируются параллельно, выбранный — первым, чтобы видимый
     * превью обновлялся быстрее остальных. Повторный вызов отменяет
     * незавершённый прогон: без отмены быстрые тапы оставляли бы гонку,
     * где устаревшие настройки перезаписывают свежие миниатюры.
     */
    private fun renderThumbnails(resume: Resume) {
        thumbnailsJob?.cancel()
        thumbnailsJob = viewModelScope.launch {
            val s = _state.value
            val selected = s.selectedTemplate
            val bitmaps = withContext(Dispatchers.IO) {
                coroutineScope {
                    PdfTemplateType.entries
                        .sortedWith(compareBy { it != selected })
                        .map { template ->
                            async {
                                runCatching {
                                    val pdf = repository.generatePdf(
                                        resume, template, s.accentColor, s.sectionOrder,
                                    )
                                    renderFirstPage(File(pdf), THUMBNAIL_WIDTH_PX)
                                }.getOrNull()?.let { template to it }
                            }
                        }
                        .awaitAll()
                        .filterNotNull()
                        .toMap()
                }
            }
            if (bitmaps.isNotEmpty()) {
                _state.update { it.copy(thumbnails = bitmaps) }
            }
        }
    }

    private var thumbnailsJob: Job? = null

    /** Ширина растеризации мини-превью шаблонов, px. */
    private companion object {
        const val THUMBNAIL_WIDTH_PX = 480
    }

    /** PdfRenderer: первая страница PDF в Bitmap заданной ширины. */
    private fun renderFirstPage(pdfFile: File, targetWidth: Int): ImageBitmap {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            val renderer = PdfRenderer(fd)
            try {
                return renderPage(renderer, 0, targetWidth)
            } finally {
                renderer.close()
            }
        }
    }

    private var pagesRenderJob: Job? = null

    /** Ширина растеризации страницы основного предпросмотра, px. */
    private val previewPageWidthPx = 900

    /**
     * Растеризует все страницы актуального PDF для постраничного предпросмотра:
     * список из «скелетов» появляется сразу, битмапы дополняются по мере
     * готовности — первая страница видна раньше остальных. Повторный вызов
     * (смена шаблона/цвета/порядка секций) отменяет незавершённый рендер.
     */
    private fun renderDocumentPages(pdfFile: File) {
        pagesRenderJob?.cancel()
        pagesRenderJob = viewModelScope.launch {
            _state.update { it.copy(previewPages = emptyList()) }
            try {
                withContext(Dispatchers.IO) {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        .use { fd ->
                            val renderer = PdfRenderer(fd)
                            try {
                                val count = renderer.pageCount
                                _state.update { it.copy(previewPages = List(count) { null }) }
                                for (index in 0 until count) {
                                    ensureActive()
                                    val pageBitmap = renderPage(renderer, index, previewPageWidthPx)
                                    _state.update { s ->
                                        // Список могли уже сбросить повторным вызовом — не пишем в чужой
                                        if (s.previewPages.size == count) {
                                            s.copy(
                                                previewPages = s.previewPages.toMutableList()
                                                    .also { it[index] = pageBitmap })
                                        } else {
                                            s
                                        }
                                    }
                                }
                            } finally {
                                renderer.close()
                            }
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось отрендерить страницы предпросмотра", e)
                _state.update { it.copy(previewPages = emptyList()) }
            }
        }
    }

    /** PdfRenderer: страница [pageIndex] в Bitmap заданной ширины на белом фоне. */
    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, targetWidth: Int): ImageBitmap {
        val page = renderer.openPage(pageIndex)
        try {
            val ratio = page.height.toFloat() / page.width.toFloat()
            val bitmap = Bitmap.createBitmap(
                targetWidth,
                (targetWidth * ratio).toInt(),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap.asImageBitmap()
        } finally {
            page.close()
        }
    }

    private fun sharePdf() {
        if (_state.value.isPdfGenerating) {
            _state.update { it.copy(pdfGenerationError = context.localizedString(R.string.error_pdf_still_generating)) }
            return
        }

        if (!_state.value.isPdfReady) {
            _state.value.resume?.let { generatePdf(it) }
            return
        }

        _state.value.pdfPath?.let { path ->
            try {
                val file = File(path)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooserIntent = Intent.createChooser(
                    intent,
                    context.localizedString(R.string.share_resume_chooser)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        pdfGenerationError = context.localizedString(
                            R.string.error_share_pdf_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        } ?: run {
            _state.update { it.copy(pdfGenerationError = context.localizedString(R.string.error_pdf_not_found)) }
        }
    }

    private fun downloadPdf() {
        if (_state.value.isPdfGenerating) {
            _state.update { it.copy(pdfGenerationError = context.localizedString(R.string.error_pdf_still_generating)) }
            return
        }

        if (!_state.value.isPdfReady) {
            _state.value.resume?.let { generatePdf(it) }
            return
        }

        viewModelScope.launch {
            val path = _state.value.pdfPath
            if (path == null) {
                _state.update { it.copy(pdfGenerationError = context.localizedString(R.string.error_pdf_not_found)) }
                return@launch
            }

            try {
                val destinationFile = withContext(Dispatchers.IO) {
                    val sourceFile = File(path)
                    val downloadsDir = context.getExternalFilesDir(null)
                        ?: throw IllegalStateException("No external files dir")
                    val fileName =
                        "resume_${_state.value.resume?.id ?: System.currentTimeMillis()}.pdf"
                    val dest = File(downloadsDir, fileName)
                    sourceFile.copyTo(dest, overwrite = true)
                    dest
                }

                _state.update { it.copy(pdfGenerationError = null) }

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            destinationFile
                        ), "application/pdf"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        pdfGenerationError = context.localizedString(
                            R.string.error_save_pdf_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    /** Тип документа для [exportDocument]. */
    private enum class ExportKind { DOCX, CSV }

    /**
     * Экспорт DOCX или CSV: генерация → сохранение в загрузки → сообщение.
     * Прежние exportDocx/exportCsv отличались только функцией репозитория,
     * mime и строками сообщений.
     */
    private fun exportDocument(kind: ExportKind) {
        val resume = _state.value.resume ?: return
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportMessage = null) }
            try {
                val saved = withContext(Dispatchers.IO) {
                    when (kind) {
                        ExportKind.DOCX -> com.bober.autcsv.core.utils.DownloadsSaver.saveFile(
                            context,
                            fileNameFor(resume, "docx"),
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            File(repository.exportDocx(resume)),
                        )

                        ExportKind.CSV -> com.bober.autcsv.core.utils.DownloadsSaver.saveFile(
                            context,
                            fileNameFor(resume, "csv"),
                            "text/csv",
                            File(repository.exportCsv(resume)),
                        )
                    }
                }
                val successRes =
                    if (kind == ExportKind.DOCX) R.string.success_docx_saved else R.string.success_csv_saved
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = context.localizedString(successRes, saved)
                    )
                }
            } catch (e: Exception) {
                val label = if (kind == ExportKind.DOCX) "DOCX" else "CSV"
                LlmLogger.logError("Не удалось экспортировать $label", e)
                val errorRes =
                    if (kind == ExportKind.DOCX) R.string.error_create_docx_fmt else R.string.error_create_csv_fmt
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = context.localizedString(errorRes, e.localizedMessage ?: "")
                    )
                }
            }
        }
    }

    /** Имя файла для экспорта: staffii-фамилия-дата.ext */
    private fun fileNameFor(resume: Resume, extension: String): String {
        val name = resume.personalInfo.fullName
            .split(" ")
            .firstOrNull()?.filter { it.isLetterOrDigit() }?.lowercase()?.takeIf { it.isNotBlank() }
            ?: "resume"
        val stamp = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        return "staffii-$name-$stamp.$extension"
    }

    private fun analyzeResume() {
        if (_state.value.resume == null) {
            _state.update { it.copy(analyzeError = context.localizedString(R.string.error_no_resume_for_analysis)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analyzeError = null) }
            try {
                val result = repository.analyzeResume(_state.value.resume!!)
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = null,
                        resume = result,
                        navigateToAnalysisResumeId = result.id
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = context.localizedString(
                            R.string.error_analyze_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun applyAiSuggestions() {
        // TODO: Implement applying AI suggestions
        _state.update { it.copy(aiSuggestionsApplied = true) }
    }

    /**
     * LLM-перевод резюме на целевой язык («ru»/«en»): создаёт НОВОЕ резюме —
     * копию с переведёнными текстами (фото переносится через дубликат),
     * оригинал не изменяется. Результат — баннер с именем созданной версии.
     */
    fun translateResume(targetLanguageCode: String) {
        val source = _state.value.resume ?: return
        if (_state.value.isTranslating) return

        viewModelScope.launch {
            _state.update { it.copy(isTranslating = true, exportMessage = null) }
            try {
                val translatedTexts = repository.translateResume(source, targetLanguageCode)

                // Дубликат даёт новый id, скопированное фото и чистые статус/избранное;
                // поверх накладываем переведённые тексты.
                val baseCopy = repository.duplicateResume(source.id)
                    ?: throw IllegalStateException(context.localizedString(R.string.error_resume_not_found))

                val suffixKey =
                    if (targetLanguageCode.startsWith("ru")) R.string.version_suffix_ru
                    else R.string.version_suffix_en
                val versionedName = baseCopy.personalInfo.fullName +
                        context.localizedString(suffixKey)

                // Фото берём из дубликата (файл скопирован), тексты — переведённые
                val translatedVersion = baseCopy.copy(
                    personalInfo = translatedTexts.personalInfo.copy(
                        photoUri = baseCopy.personalInfo.photoUri,
                        fullName = versionedName,
                    ),
                    professionalSkills = translatedTexts.professionalSkills,
                    projects = translatedTexts.projects,
                    summary = translatedTexts.summary,
                    lastModified = System.currentTimeMillis(),
                )

                repository.saveResume(translatedVersion)
                _state.update {
                    it.copy(
                        isTranslating = false,
                        exportMessage = context.localizedString(
                            R.string.translate_done_fmt,
                            translatedVersion.personalInfo.fullName
                        )
                    )
                }
            } catch (e: Exception) {
                LlmLogger.logError("Перевод резюме не удался", e)
                _state.update {
                    it.copy(
                        isTranslating = false,
                        exportMessage = context.localizedString(
                            R.string.error_translate_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun exportResume() {
        _state.value.resume?.let { generatePdf(it) }
    }
}
