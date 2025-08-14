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

class MinimalistTemplate : PdfTemplate() {
    private val subtleGray = DeviceRgb(245, 245, 245)
    private val borderColor = DeviceRgb(200, 200, 200)
    private val accentBackground = DeviceRgb(248, 249, 250)
    private val headerBackground = DeviceRgb(250, 250, 250)

    override fun generate(resume: Resume, outputFile: File): String {
        initializeFonts()
        val (pdf, document) = createDocument(outputFile)

        try {
            // Check if resume has meaningful content
            if (!hasMeaningfulContent(resume)) {
                document.add(createHeading("Резюме не содержит данных"))
                document.add(createBodyText("Пожалуйста, заполните информацию в форме создания резюме."))
                document.close()
                return outputFile.absolutePath
            }

            // Header with minimal styling
            val headerDiv = Div()
                .setBackgroundColor(headerBackground)
                .setPadding(30f)
                .setMarginBottom(30f)

            headerDiv.add(
                Paragraph(Text(resume.personalInfo.fullName.ifBlank { "Имя не указано" }).setFont(boldFont))
                    .setFontSize(32f)
                    .setFontColor(primaryColor)
                    .setMarginBottom(5f)
            )

            if (resume.personalInfo.specialization.isNotBlank()) {
                headerDiv.add(
                    Paragraph(Text(resume.personalInfo.specialization).setFont(lightFont))
                        .setFontSize(18f)
                        .setFontColor(accentColor)
                        .setMarginBottom(20f)
                )
            }

            document.add(headerDiv)

            // Contact information in a clean horizontal layout
            if (hasContactInfo(resume)) {
                val contactTable = Table(3)
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                    .setMarginBottom(30f)

                if (resume.personalInfo.email.isNotBlank()) {
                    contactTable.addCell(createContactCell(resume.personalInfo.email))
                } else {
                    contactTable.addCell(createEmptyCell())
                }

                if (resume.personalInfo.phone.isNotBlank()) {
                    contactTable.addCell(createContactCell(resume.personalInfo.phone))
                } else {
                    contactTable.addCell(createEmptyCell())
                }

                if (resume.personalInfo.location.isNotBlank()) {
                    contactTable.addCell(createContactCell(resume.personalInfo.location))
                } else {
                    contactTable.addCell(createEmptyCell())
                }

                document.add(contactTable)
            }

            // Subtle divider
            val line = SolidLine(1f)
            line.color = borderColor
            document.add(LineSeparator(line).setMarginBottom(35f))

            // Add completeness warning if needed
            addCompletenessWarning(document, resume)

            // About Me section (ensure it is included)
            if (resume.personalInfo.aboutMe.isNotBlank()) {
                document.add(createHeading("О себе"))
                document.add(createBodyText(resume.personalInfo.aboutMe))
                document.add(Paragraph().setMarginBottom(25f))
            }

            // Experience summary in a clean layout
            if (hasExperienceInfo(resume)) {
                val summaryTable = Table(2)
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                    .setMarginBottom(35f)

                if (resume.personalInfo.totalExperience.isNotBlank()) {
                    summaryTable.addCell(createSummaryCell("Общий опыт"))
                    summaryTable.addCell(createSummaryValue(resume.personalInfo.totalExperience))
                }

                if (resume.personalInfo.specializationExperience.isNotBlank()) {
                    summaryTable.addCell(createSummaryCell("Опыт в специализации"))
                    summaryTable.addCell(createSummaryValue(resume.personalInfo.specializationExperience))
                }

                document.add(summaryTable)
            }

            // Summary if available
            if (resume.summary.isNotBlank()) {
                document.add(createMinimalSection("Профессиональное резюме"))
                document.add(createBodyText(resume.summary))
                document.add(createSpacing())
            }

            // Education
            if (resume.personalInfo.education.isNotBlank()) {
                document.add(createMinimalSection(getRussianTitle("education")))
                document.add(createBodyText(resume.personalInfo.education))
                document.add(createSpacing())
            }

            // Languages
            if (resume.personalInfo.languages.isNotEmpty()) {
                document.add(createMinimalSection(getRussianTitle("languages")))
                val languagesTable = createLanguagesTable(resume.personalInfo.languages)
                document.add(languagesTable)
                document.add(createSpacing())
            }

            // Technical Skills
            if (hasTechnicalSkills(resume)) {
                document.add(createMinimalSection(getRussianTitle("technical_skills")))

                // Operating Systems
                if (resume.professionalSkills.operatingSystems.isNotEmpty()) {
                    document.add(createMinimalSubsection(getRussianTitle("operating_systems")))
                    val osTable = createMinimalSkillsTable(resume.professionalSkills.operatingSystems)
                    document.add(osTable)
                }

                // Programming Languages
                if (resume.professionalSkills.programmingLanguages.isNotEmpty()) {
                    document.add(createMinimalSubsection("Языки программирования"))
                    val langTable = createMinimalSkillsTable(resume.professionalSkills.programmingLanguages)
                    document.add(langTable)
                }

                // Frameworks
                if (resume.professionalSkills.frameworks.isNotEmpty()) {
                    document.add(createMinimalSubsection("Фреймворки"))
                    val frameworkTable = createMinimalSkillsTable(resume.professionalSkills.frameworks)
                    document.add(frameworkTable)
                }

                // Libraries
                if (resume.professionalSkills.libraries.isNotEmpty()) {
                    document.add(createMinimalSubsection("Библиотеки"))
                    val libTable = createMinimalSkillsTable(resume.professionalSkills.libraries)
                    document.add(libTable)
                }

                // Databases
                if (resume.professionalSkills.databases.isNotEmpty()) {
                    document.add(createMinimalSubsection("Базы данных"))
                    val dbTable = createMinimalSkillsTable(resume.professionalSkills.databases)
                    document.add(dbTable)
                }

                // Other Technologies
                if (resume.professionalSkills.otherTechnologies.isNotEmpty()) {
                    document.add(createMinimalSubsection("Другие технологии"))
                    val otherTable = createMinimalSkillsTable(resume.professionalSkills.otherTechnologies)
                    document.add(otherTable)
                }

                // Certifications
                if (resume.professionalSkills.certifications.isNotEmpty()) {
                    document.add(createMinimalSubsection(getRussianTitle("certifications")))
                    resume.professionalSkills.certifications.forEach { cert ->
                        document.add(createBulletPoint(cert))
                    }
                }

                // Soft Skills
                if (resume.professionalSkills.softSkills.isNotEmpty()) {
                    document.add(createMinimalSubsection(getRussianTitle("soft_skills")))
                    val softSkillsTable = createMinimalSkillsTable(resume.professionalSkills.softSkills)
                    document.add(softSkillsTable)
                }
            }

            // Achievements
            if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
                document.add(createMinimalSection(getRussianTitle("achievements")))
                resume.professionalSkills.professionalAchievements.forEach { achievement ->
                    document.add(createAchievementItem(achievement))
                }
                document.add(createSpacing())
            }

            // Projects with minimal cards
            if (resume.projects.isNotEmpty()) {
                document.add(createMinimalSection(getRussianTitle("projects")))
                resume.projects.forEachIndexed { index, project ->
                    val projectCard = createMinimalProjectCard(project, index + 1)
                    document.add(projectCard)
                }
            }

            // Footer
            val footerDiv = Div()
                .setBackgroundColor(subtleGray)
                .setPadding(15f)
                .setMarginTop(30f)
            footerDiv.add(createBodyText("Резюме сгенерировано с помощью AutCSV • ${java.time.LocalDate.now()}"))
            document.add(footerDiv)

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
               resume.personalInfo.languages.isNotEmpty() ||
               resume.professionalSkills.operatingSystems.isNotEmpty() ||
               resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
               resume.professionalSkills.certifications.isNotEmpty() ||
               resume.professionalSkills.softSkills.isNotEmpty() ||
               resume.professionalSkills.professionalAchievements.isNotEmpty() ||
               resume.projects.isNotEmpty() ||
               resume.summary.isNotBlank()
    }

    override fun addCompletenessWarning(document: com.itextpdf.layout.Document, resume: Resume) {
        val completeness = getResumeCompleteness(resume)
        if (completeness < 50) {
            val warningText = Text("⚠️ Внимание: Резюме заполнено только на $completeness%. Рекомендуется добавить больше информации для повышения эффективности.").setFont(lightFont).setFontColor(DeviceRgb(255, 140, 0))
            document.add(Paragraph(warningText).setFontSize(10f).setMarginBottom(15f))
        }
    }

    override fun createHeading(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(24f)
            .setMarginBottom(15f)
    }

    override fun createBodyText(text: String): Paragraph {
        val textElement = Text(text).setFont(lightFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(12f)
            .setMarginBottom(15f)
    }

    override fun createBulletPoint(text: String): Paragraph {
        val textElement = Text("• $text").setFont(lightFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(15f)
            .setMarginBottom(5f)
    }

    override fun createAchievementItem(text: String): Paragraph {
        val textElement = Text("✓ $text").setFont(lightFont).setFontColor(secondaryColor)
        return Paragraph(textElement)
            .setFontSize(11f)
            .setMarginLeft(15f)
            .setMarginBottom(3f)
    }

    override fun createSkillTag(text: String): Paragraph {
        val textElement = Text(text).setFont(lightFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(10f)
            .setMargin(0f)
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

    private fun createContactCell(text: String): Cell {
        val textElement = Text(text).setFont(lightFont).setFontColor(primaryColor)
        return Cell().setBorder(null)
            .setTextAlignment(TextAlignment.CENTER)
            .add(
                Paragraph(textElement)
                    .setFontSize(11f)
                    .setMargin(0f)
            )
    }

    private fun createEmptyCell(): Cell {
        return Cell().setBorder(null)
    }

    private fun createSummaryCell(text: String): Cell {
        val textElement = Text(text).setFont(regularFont).setFontColor(lightGray)
        return Cell().setBorder(null)
            .add(
                Paragraph(textElement)
                    .setFontSize(11f)
                    .setMarginBottom(3f)
            )
    }

    private fun createSummaryValue(text: String): Cell {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Cell().setBorder(null)
            .add(
                Paragraph(textElement)
                    .setFontSize(12f)
                    .setMarginBottom(0f)
            )
    }

    private fun createMinimalSection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(primaryColor)
        return Paragraph(textElement)
            .setFontSize(18f)
            .setMarginTop(25f)
            .setMarginBottom(15f)
    }

    private fun createMinimalSubsection(text: String): Paragraph {
        val textElement = Text(text).setFont(boldFont).setFontColor(accentColor)
        return Paragraph(textElement)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(10f)
    }

    private fun createLanguagesTable(languages: List<com.bober.autcsv.domain.model.Language>): Table {
        val table = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        languages.forEach { lang ->
            table.addCell(createSummaryCell(lang.name))
            table.addCell(createSummaryValue(lang.level))
        }

        return table
    }

    private fun createMinimalSkillsTable(skills: List<String>): Table {
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
                    .setBackgroundColor(accentBackground)
                    .setPadding(8f)
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

    private fun createMinimalProjectCard(project: com.bober.autcsv.domain.model.Project, projectNumber: Int): Div {
        val projectCard = Div()
            .setBackgroundColor(accentBackground)
            .setPadding(20f)
            .setMarginBottom(20f)

        // Project header
        projectCard.add(createMinimalSubsection("Проект $projectNumber: ${project.name}"))
        
        // Project details table
        val detailsTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        if (project.role.isNotBlank()) {
            detailsTable.addCell(createSummaryCell(getRussianTitle("role")))
            detailsTable.addCell(createSummaryValue(project.role))
        }

        if (project.duration.isNotBlank()) {
            detailsTable.addCell(createSummaryCell(getRussianTitle("duration")))
            detailsTable.addCell(createSummaryValue(project.duration))
        }

        if (project.teamSize.isNotBlank()) {
            detailsTable.addCell(createSummaryCell(getRussianTitle("team_size")))
            detailsTable.addCell(createSummaryValue(project.teamSize))
        }

        if (project.technologies.isNotEmpty()) {
            detailsTable.addCell(createSummaryCell(getRussianTitle("technologies")))
            detailsTable.addCell(createSummaryValue(project.technologies.joinToString(", ")))
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

    private fun createSpacing(): Paragraph {
        return Paragraph().setMarginBottom(20f)
    }
} 