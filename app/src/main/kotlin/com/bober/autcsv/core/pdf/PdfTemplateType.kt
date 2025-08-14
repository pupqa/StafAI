package com.bober.autcsv.core.pdf

enum class PdfTemplateType {
    MODERN,
    CREATIVE,
    MINIMALIST,
    PROFESSIONAL;

    fun getTemplate(): PdfTemplate = when (this) {
        MODERN -> AndroidPdfTemplate(AndroidPdfTemplate.Style.MODERN)
        CREATIVE -> AndroidPdfTemplate(AndroidPdfTemplate.Style.CREATIVE)
        MINIMALIST -> AndroidPdfTemplate(AndroidPdfTemplate.Style.MINIMALIST)
        PROFESSIONAL -> AndroidPdfTemplate(AndroidPdfTemplate.Style.PROFESSIONAL)
    }

    companion object {
        fun getDefault() = PROFESSIONAL
    }
} 