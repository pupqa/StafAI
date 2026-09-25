package com.bober.autcsv.core.pdf

import android.content.Context

enum class PdfTemplateType {
    MODERN,
    CREATIVE,
    MINIMALIST,
    PROFESSIONAL,
    SIDEBAR;

    fun getTemplate(
        context: Context,
        accentColor: Int? = null,
        sectionOrder: List<PdfSection> = PdfSection.defaultOrder(),
        embedResumeData: Boolean = false,
    ): PdfTemplate = when (this) {
        MODERN -> AndroidPdfTemplate(
            context,
            AndroidPdfTemplate.Style.MODERN,
            accentColor,
            sectionOrder,
            embedResumeData
        )

        CREATIVE -> AndroidPdfTemplate(
            context,
            AndroidPdfTemplate.Style.CREATIVE,
            accentColor,
            sectionOrder,
            embedResumeData
        )

        MINIMALIST -> AndroidPdfTemplate(
            context,
            AndroidPdfTemplate.Style.MINIMALIST,
            accentColor,
            sectionOrder,
            embedResumeData
        )

        PROFESSIONAL -> AndroidPdfTemplate(
            context,
            AndroidPdfTemplate.Style.PROFESSIONAL,
            accentColor,
            sectionOrder,
            embedResumeData
        )

        SIDEBAR -> AndroidPdfTemplate(
            context,
            AndroidPdfTemplate.Style.SIDEBAR,
            accentColor,
            sectionOrder,
            embedResumeData
        )
    }

    companion object {
        fun getDefault() = PROFESSIONAL
    }
}
