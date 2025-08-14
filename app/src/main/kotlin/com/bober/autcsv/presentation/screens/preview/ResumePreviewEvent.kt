package com.bober.autcsv.presentation.screens.preview

import com.bober.autcsv.core.pdf.PdfTemplateType

/**
 * UI-события экрана предпросмотра резюме.
 */
sealed class ResumePreviewEvent {
    data class LoadResume(val id: String) : ResumePreviewEvent()
    data class SelectTemplate(val templateType: PdfTemplateType) : ResumePreviewEvent()
    object SharePdf : ResumePreviewEvent()
    object DownloadPdf : ResumePreviewEvent()
    object AnalyzeResume : ResumePreviewEvent()
    object ApplyAiSuggestions : ResumePreviewEvent()
    object ExportResume : ResumePreviewEvent()
} 