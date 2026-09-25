package com.bober.autcsv.presentation.screens.preview

import com.bober.autcsv.core.pdf.PdfSection
import com.bober.autcsv.core.pdf.PdfTemplateType

/**
 * UI-события экрана предпросмотра резюме.
 */
sealed class ResumePreviewEvent {
    data class LoadResume(val id: String) : ResumePreviewEvent()
    data class SelectTemplate(val templateType: PdfTemplateType) : ResumePreviewEvent()

    /** Выбор акцентного цвета; null = фирменный цвет шаблона (№18). */
    data class SelectAccentColor(val color: Int?) : ResumePreviewEvent()

    /** Смена порядка секций PDF: перенос секции вверх/вниз (№24). */
    data class MoveSection(val section: PdfSection, val up: Boolean) : ResumePreviewEvent()

    object SharePdf : ResumePreviewEvent()
    object DownloadPdf : ResumePreviewEvent()
    object PrintPdf : ResumePreviewEvent()
    object ExportDocx : ResumePreviewEvent()
    object ExportCsv : ResumePreviewEvent()
    object AnalyzeResume : ResumePreviewEvent()
    object ApplyAiSuggestions : ResumePreviewEvent()
    object ExportResume : ResumePreviewEvent()
}