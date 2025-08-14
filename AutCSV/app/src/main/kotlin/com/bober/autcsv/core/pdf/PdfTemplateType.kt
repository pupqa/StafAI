package com.bober.autcsv.core.pdf

enum class PdfTemplateType {
    MODERN,
    CREATIVE,
    MINIMALIST,
    PROFESSIONAL,
    TEST;

    fun getTemplate(): PdfTemplate {
        return when (this) {
            MODERN -> ModernTemplate()
            CREATIVE -> CreativeTemplate()
            MINIMALIST -> MinimalistTemplate()
            PROFESSIONAL -> ProfessionalTemplate()
            TEST -> TestTemplate()
        }
    }

    companion object {
        fun getDefault() = PROFESSIONAL
    }
} 