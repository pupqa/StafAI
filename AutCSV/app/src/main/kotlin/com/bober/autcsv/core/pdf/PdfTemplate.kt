package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.Resume
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import java.io.File

sealed class PdfTemplate {
    // Контрастная палитра цветов для лучшей читаемости
    protected val primaryColor = DeviceRgb(0, 0, 0)        // Чистый черный для основного текста
    protected val accentColor = DeviceRgb(0, 102, 204)      // Темно-синий для акцентов
    protected val secondaryColor = DeviceRgb(0, 128, 0)     // Темно-зеленый для достижений
    protected open val lightGray = DeviceRgb(128, 128, 128)      // Средне-серый для вторичного текста
    protected val veryLightGray = DeviceRgb(240, 240, 240)  // Очень светло-серый для фонов
    protected open val darkGray = DeviceRgb(64, 64, 64)          // Темно-серый для контактов

    // Общие шрифты
    protected lateinit var regularFont: PdfFont
    protected lateinit var boldFont: PdfFont
    protected lateinit var lightFont: PdfFont

    protected fun initializeFonts() {
        regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        lightFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE)
    }

    // Заголовки разделов на русском
    protected val russianTitles = mapOf(
        "personal_info" to "Личная информация",
        "contact_info" to "Контактная информация",
        "experience" to "Опыт работы",
        "education" to "Образование",
        "languages" to "Языки",
        "technical_skills" to "Технические навыки",
        "operating_systems" to "Операционные системы",
        "technologies" to "Технологии",
        "certifications" to "Сертификации",
        "soft_skills" to "Гибкие навыки",
        "achievements" to "Ключевые достижения",
        "projects" to "Проектный опыт",
        "role" to "Роль",
        "duration" to "Продолжительность",
        "description" to "Описание",
        "responsibilities" to "Обязанности",
        "project_achievements" to "Достижения в проекте",
        "team_size" to "Размер команды",
        "industry" to "Отрасль"
    )

    // Вспомогательные функции для единообразного стиля
    protected open fun createHeading(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(24f)
            .setMarginBottom(8f)
    }

    protected fun createSubheading(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(accentColor)
        return Paragraph(textElement)
            .setFontSize(18f)
            .setMarginBottom(6f)
    }

    protected fun createSectionTitle(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(16f)
            .setMarginTop(20f)
            .setMarginBottom(12f)
    }

    protected fun createSubsectionTitle(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(accentColor)
        return Paragraph(textElement)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(8f)
    }

    protected open fun createBodyText(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginBottom(6f)
    }

    protected open fun createBulletPoint(text: String): Paragraph {
        val textElement = Text("• $text").setFont(regularFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(20f)
            .setMarginBottom(4f)
    }

    protected fun createContactInfo(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(darkGray)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginBottom(3f)
    }

    protected open fun createSkillTag(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(accentColor)
        return Paragraph(textElement)
            .setFontSize(10f)
            .setMarginBottom(3f)
    }

    protected open fun createAchievementItem(text: String): Paragraph {
        val textElement = Text("✓ $text").setFont(regularFont).setFontColor(secondaryColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(15f)
            .setMarginBottom(4f)
    }

    // Вспомогательная функция: есть ли в резюме значимое содержание
    protected open fun hasMeaningfulContent(resume: Resume): Boolean {
        return resume.personalInfo.fullName.isNotBlank() ||
                resume.personalInfo.specialization.isNotBlank() ||
                resume.personalInfo.totalExperience.isNotBlank() ||
                resume.personalInfo.aboutMe.isNotBlank() ||
                resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
                resume.projects.isNotEmpty() ||
                resume.summary.isNotBlank()
    }

    // Helper function to get resume completeness score
    protected open fun getResumeCompleteness(resume: Resume): Int {
        var score = 0
        var totalFields = 0

        // Персональная информация (вес 30%)
        totalFields += 7
        if (resume.personalInfo.fullName.isNotBlank()) score++
        if (resume.personalInfo.specialization.isNotBlank()) score++
        if (resume.personalInfo.totalExperience.isNotBlank()) score++
        if (resume.personalInfo.email.isNotBlank()) score++
        if (resume.personalInfo.phone.isNotBlank()) score++
        if (resume.personalInfo.education.isNotBlank()) score++
        if (resume.personalInfo.aboutMe.isNotBlank()) score++

        // Профессиональные навыки (вес 40%)
        totalFields += 5
        if (resume.professionalSkills.getAllTechnologies().isNotEmpty()) score++
        if (resume.professionalSkills.professionalAchievements.isNotEmpty()) score++
        if (resume.professionalSkills.softSkills.isNotEmpty()) score++
        if (resume.professionalSkills.certifications.isNotEmpty()) score++
        if (resume.professionalSkills.operatingSystems.isNotEmpty()) score++

        // Проекты (вес 30%)
        totalFields += 1
        if (resume.projects.isNotEmpty()) score++

        return (score * 100) / totalFields
    }

    // Вспомогательная функция: добавить предупреждение о неполноте при необходимости
    protected open fun addCompletenessWarning(document: Document, resume: Resume) {
        val completeness = getResumeCompleteness(resume)
        if (completeness < 50) {
            val warningText = Text("Рекомендация: резюме заполнено на $completeness%. Добавьте больше информации для повышения эффективности.")
                .setFont(lightFont)
                .setFontColor(DeviceRgb(255, 140, 0))
            document.add(Paragraph(warningText).setFontSize(10f).setMarginBottom(15f))
        }
    }

    abstract fun generate(resume: Resume, outputFile: File): String

    protected fun createDocument(outputFile: File): Pair<PdfDocument, Document> {
        val writer = PdfWriter(outputFile)
        val pdf = PdfDocument(writer)
        pdf.defaultPageSize = PageSize.A4
        val document = Document(pdf)
        document.setMargins(40f, 40f, 40f, 40f)
        return Pair(pdf, document)
    }

    // Вспомогательная функция: получить русский заголовок по ключу
    protected fun getRussianTitle(key: String): String {
        return russianTitles[key] ?: key
    }
} 