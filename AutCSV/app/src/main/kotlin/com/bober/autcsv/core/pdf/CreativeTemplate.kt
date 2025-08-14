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

class CreativeTemplate : PdfTemplate() {
    private val accentBackground = DeviceRgb(240, 240, 240)
    private val sidebarColor = DeviceRgb(33, 33, 33)
    private val sidebarTextColor = DeviceRgb(255, 255, 255)
    private val cardBackground = DeviceRgb(252, 252, 252)
    private val highlightColor = DeviceRgb(255, 193, 7)
    private val gradientStart = DeviceRgb(41, 128, 185)
    private val gradientEnd = DeviceRgb(52, 152, 219)

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

            // Создаем основную таблицу с боковой колонкой
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
                .useAllAvailableWidth()
                .setMargins(0f, 0f, 0f, 0f)

            // Левая боковая колонка
            val sidebar = Cell()
                .setBackgroundColor(sidebarColor)
                .setPadding(25f)
                .setBorder(null)

            // Персональная информация в боковой колонке
            sidebar.add(createSidebarHeading(resume.personalInfo.fullName.ifBlank { "Имя не указано" }))
            if (resume.personalInfo.specialization.isNotBlank()) {
                sidebar.add(createSidebarSubheading(resume.personalInfo.specialization))
            }

            // Контактная информация в боковой колонке (без эмодзи)
            if (hasContactInfo(resume)) {
                sidebar.add(createSidebarSection("Контакты"))
                
                if (resume.personalInfo.email.isNotBlank()) {
                    sidebar.add(createSidebarContact(resume.personalInfo.email))
                }
                if (resume.personalInfo.phone.isNotBlank()) {
                    sidebar.add(createSidebarContact(resume.personalInfo.phone))
                }
                if (resume.personalInfo.location.isNotBlank()) {
                    sidebar.add(createSidebarContact(resume.personalInfo.location))
                }
            }

            // Сводка по опыту
            if (hasExperienceInfo(resume)) {
                sidebar.add(createSidebarSection(getRussianTitle("experience")))
                
                if (resume.personalInfo.totalExperience.isNotBlank()) {
                    sidebar.add(createSidebarText("Общий: ${resume.personalInfo.totalExperience}"))
                }
                if (resume.personalInfo.specializationExperience.isNotBlank()) {
                    sidebar.add(createSidebarText("В специализации: ${resume.personalInfo.specializationExperience}"))
                }
            }

            // Languages
            if (resume.personalInfo.languages.isNotEmpty()) {
                sidebar.add(createSidebarSection(getRussianTitle("languages")))
                resume.personalInfo.languages.forEach { lang ->
                    sidebar.add(createSidebarBullet("${lang.name} - ${lang.level}"))
                }
            }

            // Operating Systems
            if (resume.professionalSkills.operatingSystems.isNotEmpty()) {
                sidebar.add(createSidebarSection(getRussianTitle("operating_systems")))
                resume.professionalSkills.operatingSystems.forEach { os ->
                    sidebar.add(createSidebarBullet(os))
                }
            }

            // Certifications
            if (resume.professionalSkills.certifications.isNotEmpty()) {
                sidebar.add(createSidebarSection(getRussianTitle("certifications")))
                resume.professionalSkills.certifications.forEach { cert ->
                    sidebar.add(createSidebarBullet(cert))
                }
            }

            // Add sidebar to main table
            mainTable.addCell(sidebar)

            // Main content area
            val mainContent = Cell()
                .setPadding(30f)
                .setBorder(null)

            // Add completeness warning if needed
            addCompletenessWarning(mainContent, resume)

            // Summary and About Me
            if (resume.summary.isNotBlank()) {
                mainContent.add(createCreativeSection("Профессиональное резюме"))
                mainContent.add(createBodyText(resume.summary))
                mainContent.add(createSpacing())
            }
            if (resume.personalInfo.aboutMe.isNotBlank()) {
                mainContent.add(createCreativeSection("О себе"))
                mainContent.add(createBodyText(resume.personalInfo.aboutMe))
                mainContent.add(createSpacing())
            }

            // Education
            if (resume.personalInfo.education.isNotBlank()) {
                mainContent.add(createCreativeSection(getRussianTitle("education")))
                mainContent.add(createBodyText(resume.personalInfo.education))
                mainContent.add(createSpacing())
            }

            // Programming Languages
            if (resume.professionalSkills.programmingLanguages.isNotEmpty()) {
                mainContent.add(createCreativeSection("Языки программирования"))
                val langTable = createCreativeSkillsGrid(resume.professionalSkills.programmingLanguages)
                mainContent.add(langTable)
                mainContent.add(createSpacing())
            }

            // Frameworks
            if (resume.professionalSkills.frameworks.isNotEmpty()) {
                mainContent.add(createCreativeSection("Фреймворки"))
                val frameworkTable = createCreativeSkillsGrid(resume.professionalSkills.frameworks)
                mainContent.add(frameworkTable)
                mainContent.add(createSpacing())
            }

            // Libraries
            if (resume.professionalSkills.libraries.isNotEmpty()) {
                mainContent.add(createCreativeSection("Библиотеки"))
                val libTable = createCreativeSkillsGrid(resume.professionalSkills.libraries)
                mainContent.add(libTable)
                mainContent.add(createSpacing())
            }

            // Databases
            if (resume.professionalSkills.databases.isNotEmpty()) {
                mainContent.add(createCreativeSection("Базы данных"))
                val dbTable = createCreativeSkillsGrid(resume.professionalSkills.databases)
                mainContent.add(dbTable)
                mainContent.add(createSpacing())
            }

            // Other Technologies
            if (resume.professionalSkills.otherTechnologies.isNotEmpty()) {
                mainContent.add(createCreativeSection("Другие технологии"))
                val otherTable = createCreativeSkillsGrid(resume.professionalSkills.otherTechnologies)
                mainContent.add(otherTable)
                mainContent.add(createSpacing())
            }

            // Soft Skills
            if (resume.professionalSkills.softSkills.isNotEmpty()) {
                mainContent.add(createCreativeSection(getRussianTitle("soft_skills")))
                val softSkillsTable = createCreativeSkillsGrid(resume.professionalSkills.softSkills)
                mainContent.add(softSkillsTable)
                mainContent.add(createSpacing())
            }

            // Achievements
            if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
                mainContent.add(createCreativeSection(getRussianTitle("achievements")))
                resume.professionalSkills.professionalAchievements.forEach { achievement ->
                    mainContent.add(createAchievementItem(achievement))
                }
                mainContent.add(createSpacing())
            }

            // Projects
            if (resume.projects.isNotEmpty()) {
                mainContent.add(createCreativeSection(getRussianTitle("projects")))
                resume.projects.forEachIndexed { index, project ->
                    val projectCard = createCreativeProjectCard(project, index + 1)
                    mainContent.add(projectCard)
                }
            }

            // Footer
            val footerDiv = Div()
                .setBackgroundColor(accentBackground)
                .setPadding(15f)
                .setMarginTop(20f)
            footerDiv.add(createBodyText("Резюме сгенерировано с помощью AutCSV • ${java.time.LocalDate.now()}"))
            mainContent.add(footerDiv)

            // Add main content to table
            mainTable.addCell(mainContent)

            // Add table to document
            document.add(mainTable)

            document.close()
            return outputFile.absolutePath

        } catch (e: Exception) {
            pdf.close()
            throw e
        }
    }

    override fun hasMeaningfulContent(resume: Resume): Boolean {
        return resume.personalInfo.fullName.isNotBlank() ||
               resume.personalInfo.specialization.isNotBlank() ||
               resume.personalInfo.email.isNotBlank() ||
               resume.personalInfo.phone.isNotBlank() ||
               resume.personalInfo.location.isNotBlank() ||
               resume.personalInfo.totalExperience.isNotBlank() ||
               resume.personalInfo.specializationExperience.isNotBlank() ||
               resume.personalInfo.education.isNotBlank() ||
               resume.summary.isNotBlank() ||
               resume.projects.isNotEmpty() ||
               resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
               resume.professionalSkills.softSkills.isNotEmpty() ||
               resume.professionalSkills.professionalAchievements.isNotEmpty() ||
               resume.personalInfo.languages.isNotEmpty() ||
               resume.professionalSkills.operatingSystems.isNotEmpty() ||
               resume.professionalSkills.certifications.isNotEmpty()
    }

    override fun getResumeCompleteness(resume: Resume): Int {
        var completeness = 0
        if (resume.personalInfo.fullName.isNotBlank()) completeness += 10
        if (resume.personalInfo.specialization.isNotBlank()) completeness += 10
        if (resume.personalInfo.email.isNotBlank()) completeness += 10
        if (resume.personalInfo.phone.isNotBlank()) completeness += 10
        if (resume.personalInfo.location.isNotBlank()) completeness += 10
        if (resume.personalInfo.totalExperience.isNotBlank()) completeness += 10
        if (resume.personalInfo.specializationExperience.isNotBlank()) completeness += 10
        if (resume.personalInfo.education.isNotBlank()) completeness += 10
        if (resume.summary.isNotBlank()) completeness += 10
        if (resume.projects.isNotEmpty()) completeness += 10
        if (resume.professionalSkills.getAllTechnologies().isNotEmpty()) completeness += 10
        if (resume.professionalSkills.softSkills.isNotEmpty()) completeness += 10
        if (resume.professionalSkills.professionalAchievements.isNotEmpty()) completeness += 10
        if (resume.personalInfo.languages.isNotEmpty()) completeness += 10
        if (resume.professionalSkills.operatingSystems.isNotEmpty()) completeness += 10
        if (resume.professionalSkills.certifications.isNotEmpty()) completeness += 10
        return (completeness / 10) * 10
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

    private fun addCompletenessWarning(cell: Cell, resume: Resume) {
        val completeness = getResumeCompleteness(resume)
        if (completeness < 50) {
            val warningText = Text("⚠️ Внимание: Резюме заполнено только на $completeness%. Рекомендуется добавить больше информации для повышения эффективности.").setFont(lightFont).setFontColor(DeviceRgb(255, 140, 0))
            cell.add(Paragraph(warningText).setFontSize(10f).setMarginBottom(15f))
        }
    }

    private fun createSidebarHeading(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(sidebarTextColor)
        return Paragraph(textElement)
            .setFontSize(20f)
            .setMarginBottom(8f)
    }

    private fun createSidebarSubheading(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(sidebarTextColor)
        return Paragraph(textElement)
            .setFontSize(16f)
            .setMarginBottom(25f)
    }

    private fun createSidebarSection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(highlightColor)
        return Paragraph(textElement)
            .setFontSize(14f)
            .setMarginTop(20f)
            .setMarginBottom(8f)
    }

    private fun createSidebarText(text: String): Paragraph {
        val textElement = Text(text).setFont(lightFont).setFontColor(sidebarTextColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginBottom(3f)
    }

    private fun createSidebarContact(text: String): Paragraph {
        val textElement = Text(text).setFont(regularFont).setFontColor(sidebarTextColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginBottom(5f)
    }

    private fun createSidebarBullet(text: String): Paragraph {
        val textElement = Text("• $text").setFont(lightFont).setFontColor(sidebarTextColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(10f)
            .setMarginBottom(3f)
    }

    private fun createCreativeSection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(18f)
            .setMarginBottom(15f)
    }

    private fun createCreativeSkillsGrid(skills: List<String>): Table {
        val columns = when {
            skills.size <= 2 -> 2
            skills.size <= 4 -> 3
            else -> 4
        }
        
        val table = Table(columns)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        skills.forEach { skill ->
            table.addCell(
                Cell().setBorder(null)
                    .setBackgroundColor(cardBackground)
                    .setPadding(10f)
                    .add(createSkillTag(skill))
            )
        }

        // Fill remaining cells if needed
        val remainingCells = columns - (skills.size % columns)
        if (remainingCells < columns) {
            repeat(remainingCells) {
                table.addCell(Cell().setBorder(null))
            }
        }

        return table
    }

    private fun createCreativeProjectCard(project: com.bober.autcsv.domain.model.Project, projectNumber: Int): Div {
        val projectCard = Div()
            .setBackgroundColor(cardBackground)
            .setPadding(20f)
            .setMarginBottom(20f)

        // Project header with accent line
        val headerDiv = Div()
        headerDiv.add(createSubheading("Проект $projectNumber: ${project.name}"))
        
        // Add accent line
        val line = SolidLine(3f)
        line.color = accentColor
        headerDiv.add(LineSeparator(line).setMarginTop(5f).setMarginBottom(15f))
        
        projectCard.add(headerDiv)

        // Project details in a creative layout
        val detailsTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        if (project.role.isNotBlank()) {
            detailsTable.addCell(createCreativeDetailCell(getRussianTitle("role")))
            detailsTable.addCell(createCreativeDetailCell(project.role))
        }

        if (project.duration.isNotBlank()) {
            detailsTable.addCell(createCreativeDetailCell(getRussianTitle("duration")))
            detailsTable.addCell(createCreativeDetailCell(project.duration))
        }

        if (project.teamSize.isNotBlank()) {
            detailsTable.addCell(createCreativeDetailCell(getRussianTitle("team_size")))
            detailsTable.addCell(createCreativeDetailCell(project.teamSize))
        }

        if (project.technologies.isNotEmpty()) {
            detailsTable.addCell(createCreativeDetailCell(getRussianTitle("technologies")))
            detailsTable.addCell(createCreativeDetailCell(project.technologies.joinToString(", ")))
        }

        projectCard.add(detailsTable)

        // Description
        if (project.description.isNotBlank()) {
            projectCard.add(createBodyText("${getRussianTitle("description")}:"))
            projectCard.add(createBodyText(project.description))
        }

        // Responsibilities
        if (project.responsibilities.isNotEmpty()) {
            projectCard.add(createBodyText("${getRussianTitle("responsibilities")}:"))
            project.responsibilities.forEach { resp ->
                projectCard.add(createBulletPoint(resp))
            }
        }

        return projectCard
    }

    private fun createCreativeDetailCell(text: String): Cell {
        return Cell()
            .setBorder(null)
            .setPadding(5f)
            .add(createBodyText(text))
    }

    private fun createSpacing(): Paragraph {
        return Paragraph().setMarginBottom(25f)
    }
} 