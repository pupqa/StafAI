package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.Resume
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File

class ProfessionalTemplate : PdfTemplate() {
    private val primaryBlue = DeviceRgb(25, 118, 210)
    private val secondaryBlue = DeviceRgb(66, 165, 245)
    private val accentOrange = DeviceRgb(255, 152, 0)
    private val lightBlue = DeviceRgb(227, 242, 253)
    override val darkGray = DeviceRgb(55, 71, 79)
    private val mediumGray = DeviceRgb(117, 117, 117)
    override val lightGray = DeviceRgb(245, 245, 245)

    override fun generate(resume: Resume, outputFile: File): String {
        initializeFonts()
        val (pdf, document) = createDocument(outputFile)

        try {
            // Проверяем, содержит ли резюме значимую информацию
            if (!hasMeaningfulContent(resume)) {
                document.add(createHeading("Резюме не содержит данных"))
                document.add(createBodyText("Пожалуйста, заполните информацию в форме создания резюме."))
                document.close()
                return outputFile.absolutePath
            }

            // Чистая шапка (без фона, акцент на типографике)
            val headerDiv = Div()
                .setPadding(8f)
                .setMarginBottom(10f)

            headerDiv.add(createProfessionalHeading(resume.personalInfo.fullName.ifBlank { "Имя не указано" }))
            if (resume.personalInfo.specialization.isNotBlank()) {
                headerDiv.add(createProfessionalSubheading(resume.personalInfo.specialization))
            }
            document.add(headerDiv)

            // Компактная строка контактов (в одну линию, без эмодзи)
            if (hasContactInfo(resume)) {
                document.add(createContactLine(resume))
            }

            // Ненавязчивый разделитель
            val line = SolidLine(1f)
            line.color = mediumGray
            document.add(LineSeparator(line).setMarginTop(8f).setMarginBottom(16f))

            // Добавляем предупреждение о неполноте, если требуется
            addCompletenessWarning(document, resume)

            // Профессиональное резюме
            if (resume.summary.isNotBlank()) {
                document.add(createProfessionalSection("Профессиональное резюме"))
                document.add(createProfessionalBodyText(resume.summary))
                document.add(createSpacing())
            }

            // Сводка по опыту
            if (hasExperienceInfo(resume)) {
                document.add(createProfessionalSection("Опыт работы"))
                
                val experienceTable = Table(2)
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                    .setMarginBottom(15f)

                if (resume.personalInfo.totalExperience.isNotBlank()) {
                    experienceTable.addCell(createProfessionalLabel("Общий опыт работы"))
                    experienceTable.addCell(createProfessionalValue(resume.personalInfo.totalExperience))
                }

                if (resume.personalInfo.specializationExperience.isNotBlank()) {
                    experienceTable.addCell(createProfessionalLabel("Опыт в специализации"))
                    experienceTable.addCell(createProfessionalValue(resume.personalInfo.specializationExperience))
                }

                document.add(experienceTable)
                document.add(createSpacing())
            }

            // Образование
            if (resume.personalInfo.education.isNotBlank()) {
                document.add(createProfessionalSection(getRussianTitle("education")))
                document.add(createProfessionalBodyText(resume.personalInfo.education))
                document.add(createSpacing())
            }

            // Языки
            if (resume.personalInfo.languages.isNotEmpty()) {
                document.add(createProfessionalSection(getRussianTitle("languages")))
                val langLine = resume.personalInfo.languages.joinToString(" • ") { "${it.name} — ${it.level}" }
                document.add(createProfessionalBodyText(langLine))
                document.add(createSpacing())
            }

            // Технические навыки (в строку, минималистичный стиль)
            if (hasTechnicalSkills(resume)) {
                document.add(createProfessionalSection("Технические навыки"))

                // Операционные системы
                if (resume.professionalSkills.operatingSystems.isNotEmpty()) {
                    document.add(createProfessionalSubsection(getRussianTitle("operating_systems")))
                    document.add(createSkillsInline(resume.professionalSkills.operatingSystems))
                }

                // Языки программирования
                if (resume.professionalSkills.programmingLanguages.isNotEmpty()) {
                    document.add(createProfessionalSubsection("Языки программирования"))
                    document.add(createSkillsInline(resume.professionalSkills.programmingLanguages))
                }

                // Фреймворки
                if (resume.professionalSkills.frameworks.isNotEmpty()) {
                    document.add(createProfessionalSubsection("Фреймворки"))
                    document.add(createSkillsInline(resume.professionalSkills.frameworks))
                }

                // Библиотеки
                if (resume.professionalSkills.libraries.isNotEmpty()) {
                    document.add(createProfessionalSubsection("Библиотеки"))
                    document.add(createSkillsInline(resume.professionalSkills.libraries))
                }

                // Базы данных
                if (resume.professionalSkills.databases.isNotEmpty()) {
                    document.add(createProfessionalSubsection("Базы данных"))
                    document.add(createSkillsInline(resume.professionalSkills.databases))
                }

                // Другие технологии
                if (resume.professionalSkills.otherTechnologies.isNotEmpty()) {
                    document.add(createProfessionalSubsection("Другие технологии"))
                    document.add(createSkillsInline(resume.professionalSkills.otherTechnologies))
                }

                // Сертификации
                if (resume.professionalSkills.certifications.isNotEmpty()) {
                    document.add(createProfessionalSubsection(getRussianTitle("certifications")))
                    resume.professionalSkills.certifications.forEach { cert ->
                        document.add(createProfessionalBullet(cert))
                    }
                }

                // Гибкие навыки
                if (resume.professionalSkills.softSkills.isNotEmpty()) {
                    document.add(createProfessionalSubsection(getRussianTitle("soft_skills")))
                    document.add(createSkillsInline(resume.professionalSkills.softSkills))
                }
            }

            // Ключевые достижения
            if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
                document.add(createProfessionalSection("Ключевые достижения"))
                resume.professionalSkills.professionalAchievements.forEach { achievement ->
                    document.add(createProfessionalAchievement(achievement))
                }
                document.add(createSpacing())
            }

            // Проекты (аккуратные блоки, без фона)
            if (resume.projects.isNotEmpty()) {
                document.add(createProfessionalSection("Проектный опыт"))
                resume.projects.forEachIndexed { index, project ->
                    val block = createProfessionalProjectBlock(project, index + 1)
                    document.add(block)
                }
            }

            // Минималистичный нижний колонтитул
            val footerLine = SolidLine(1f)
            footerLine.color = lightBlue
            document.add(LineSeparator(footerLine).setMarginTop(20f))
            document.add(createProfessionalFooterText("AutCSV • ${java.time.LocalDate.now()}"))

            document.close()
            return outputFile.absolutePath

        } catch (e: Exception) {
            pdf.close()
            throw e
        }
    }

    private fun hasContactInfo(resume: Resume): Boolean {
        return resume.personalInfo.email.isNotBlank() || 
               resume.personalInfo.phone.isNotBlank() || 
               resume.personalInfo.location.isNotBlank()
    }

    private fun hasExperienceInfo(resume: Resume): Boolean {
        return resume.personalInfo.totalExperience.isNotBlank() || 
               resume.personalInfo.specializationExperience.isNotBlank()
    }

    private fun hasTechnicalSkills(resume: Resume): Boolean {
        return resume.professionalSkills.operatingSystems.isNotEmpty() ||
               resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
               resume.professionalSkills.certifications.isNotEmpty() ||
               resume.professionalSkills.softSkills.isNotEmpty()
    }

    private fun createProfessionalHeading(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryBlue)
        return Paragraph(textElement)
            .setFontSize(28f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(8f)
    }

    private fun createProfessionalSubheading(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(darkGray)
        return Paragraph(textElement)
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(0f)
    }

    private fun createProfessionalSection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryBlue)
        return Paragraph(textElement)
            .setFontSize(16f)
            .setMarginTop(20f)
            .setMarginBottom(12f)
    }

    private fun createProfessionalSubsection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(secondaryBlue)
        return Paragraph(textElement)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(8f)
    }

    private fun createProfessionalBodyText(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(darkGray)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginBottom(8f)
    }

    private fun createContactLine(resume: Resume): Paragraph {
        val parts = mutableListOf<String>()
        if (resume.personalInfo.email.isNotBlank()) parts.add(resume.personalInfo.email)
        if (resume.personalInfo.phone.isNotBlank()) parts.add(resume.personalInfo.phone)
        if (resume.personalInfo.location.isNotBlank()) parts.add(resume.personalInfo.location)
        val textElement = Text(parts.joinToString("  •  ")).setFont(regularFont).setFontColor(mediumGray)
        return Paragraph(textElement).setFontSize(11f).setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(2f).setMarginBottom(12f)
    }

    private fun createEmptyCell(): Cell {
        return Cell().setBorder(null)
    }

    private fun createProfessionalLabel(text: String): Cell {
        val textElement = Text(text).setFont(boldFont).setFontColor(mediumGray)
        return Cell().setBorder(null)
            .add(Paragraph(textElement).setFontSize(11f).setMarginBottom(3f))
    }

    private fun createProfessionalValue(text: String): Cell {
        val textElement = Text(text).setFont(regularFont).setFontColor(darkGray)
        return Cell().setBorder(null)
            .add(Paragraph(textElement).setFontSize(12f).setMarginBottom(0f))
    }

    private fun createSkillsInline(skills: List<String>): Paragraph {
        val joined = skills.joinToString("  •  ")
        val textElement = Text(joined).setFont(regularFont).setFontColor(darkGray)
        return Paragraph(textElement).setFontSize(11f).setMarginBottom(8f)
    }

    // Removed grid to keep layout clean

    private fun createProfessionalSkillTag(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(primaryBlue)
        return Paragraph(textElement)
            .setFontSize(10f)
            .setMargin(0f)
    }

    private fun createProfessionalBullet(text: String): Paragraph {
        val textElement = Text("• $text").setFont(regularFont).setFontColor(darkGray)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(20f)
            .setMarginBottom(4f)
    }

    private fun createProfessionalAchievement(text: String): Paragraph {
        val textElement = Text("✓ $text").setFont(regularFont).setFontColor(accentOrange)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(15f)
            .setMarginBottom(4f)
    }

    private fun createProfessionalProjectBlock(project: com.bober.autcsv.domain.model.Project, projectNumber: Int): Div {
        val block = Div().setPadding(0f).setMarginBottom(16f)
        // Header
        block.add(createProfessionalSubsection("Проект $projectNumber: ${project.name}"))
        // Details inline line
        val detailParts = mutableListOf<String>()
        if (project.role.isNotBlank()) detailParts.add("Роль: ${project.role}")
        if (project.duration.isNotBlank()) detailParts.add("Срок: ${project.duration}")
        if (project.teamSize.isNotBlank()) detailParts.add("Команда: ${project.teamSize}")
        if (project.technologies.isNotEmpty()) detailParts.add("Технологии: ${project.technologies.joinToString(", ")}")
        if (detailParts.isNotEmpty()) {
            block.add(createProfessionalBodyText(detailParts.joinToString("  •  ")))
        }
        // Description
        if (project.description.isNotBlank()) {
            block.add(createProfessionalBodyText(project.description))
        }
        // Responsibilities
        if (project.responsibilities.isNotEmpty()) {
            project.responsibilities.forEach { resp ->
                block.add(createProfessionalBullet(resp))
            }
        }
        return block
    }

    private fun createProfessionalFooterText(text: String): Paragraph {
        val textElement = Text(text).setFont(lightFont).setFontColor(mediumGray)
        return Paragraph(textElement)
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMargin(0f)
    }

    private fun createSpacing(): Paragraph {
        return Paragraph().setMarginBottom(20f)
    }

    override fun addCompletenessWarning(document: com.itextpdf.layout.Document, resume: Resume) {
        val completeness = getResumeCompleteness(resume)
        if (completeness < 40) {
            val text = Text("Рекомендация: резюме заполнено на $completeness%. Добавьте больше деталей для лучшего впечатления.")
                .setFont(lightFont)
                .setFontColor(mediumGray)
            document.add(Paragraph(text).setFontSize(9f).setMarginBottom(10f))
        }
    }
} 