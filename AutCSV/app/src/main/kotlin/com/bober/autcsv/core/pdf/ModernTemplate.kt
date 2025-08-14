package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.Resume
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.element.Paragraph

class ModernTemplate : PdfTemplate() {
    private val dividerColor = DeviceRgb(200, 200, 200)
    private val cardBackground = DeviceRgb(248, 248, 248)
    private val headerBackground = DeviceRgb(245, 247, 250)

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

            // Шапка с фоновым блоком
            val headerDiv = Div()
                .setBackgroundColor(headerBackground)
                .setPadding(25f)
                .setMarginBottom(20f)

            // Имя и специализация
            headerDiv.add(createHeading(resume.personalInfo.fullName.ifBlank { "Имя не указано" }).setTextAlignment(TextAlignment.CENTER))
            if (resume.personalInfo.specialization.isNotBlank()) {
                headerDiv.add(createSubheading(resume.personalInfo.specialization).setTextAlignment(TextAlignment.CENTER))
            }
            
            document.add(headerDiv)

            // Контактная информация в структурированном виде (без эмодзи)
            if (hasContactInfo(resume)) {
                val contactTable = Table(3)
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                    .setMarginBottom(20f)

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

            // Разделитель
            val line = SolidLine(2f)
            line.color = accentColor
            document.add(LineSeparator(line).setMarginTop(15f).setMarginBottom(25f))

            // Добавляем предупреждение о неполноте, если требуется
            addCompletenessWarning(document, resume)

            // Раздел «Опыт работы»
            if (hasExperienceInfo(resume)) {
                document.add(createSectionTitle(getRussianTitle("experience")))
                
                val experienceTable = Table(2)
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                    .setMarginBottom(15f)

                if (resume.personalInfo.totalExperience.isNotBlank()) {
                    experienceTable.addCell(createDetailLabel("Общий опыт работы"))
                    experienceTable.addCell(createDetailValue(resume.personalInfo.totalExperience))
                }

                if (resume.personalInfo.specializationExperience.isNotBlank()) {
                    experienceTable.addCell(createDetailLabel("Опыт в специализации"))
                    experienceTable.addCell(createDetailValue(resume.personalInfo.specializationExperience))
                }

                document.add(experienceTable)
            }

            // Раздел «Резюме» (включая «О себе», если есть)
            if (resume.summary.isNotBlank()) {
                document.add(createSectionTitle("Профессиональное резюме"))
                document.add(createBodyText(resume.summary))
            }
            if (resume.personalInfo.aboutMe.isNotBlank()) {
                document.add(createSectionTitle("О себе"))
                document.add(createBodyText(resume.personalInfo.aboutMe))
            }

            // Раздел «Образование»
            if (resume.personalInfo.education.isNotBlank()) {
                document.add(createSectionTitle(getRussianTitle("education")))
                document.add(createBodyText(resume.personalInfo.education))
            }

            // Languages section
            if (resume.personalInfo.languages.isNotEmpty()) {
                document.add(createSectionTitle(getRussianTitle("languages")))
                val languagesTable = createLanguagesTable(resume.personalInfo.languages)
                document.add(languagesTable)
            }

            // Technical Skills section — include all categories
            if (hasTechnicalSkills(resume)) {
                document.add(createSectionTitle(getRussianTitle("technical_skills")))

                // Operating Systems
                if (resume.professionalSkills.operatingSystems.isNotEmpty()) {
                    document.add(createSubsectionTitle(getRussianTitle("operating_systems")))
                    val osTable = createSkillsTable(resume.professionalSkills.operatingSystems)
                    document.add(osTable)
                }

                // Programming Languages
                if (resume.professionalSkills.programmingLanguages.isNotEmpty()) {
                    document.add(createSubsectionTitle("Языки программирования"))
                    val langTable = createSkillsTable(resume.professionalSkills.programmingLanguages)
                    document.add(langTable)
                }

                // Frameworks
                if (resume.professionalSkills.frameworks.isNotEmpty()) {
                    document.add(createSubsectionTitle("Фреймворки"))
                    val frameworkTable = createSkillsTable(resume.professionalSkills.frameworks)
                    document.add(frameworkTable)
                }

                // Libraries
                if (resume.professionalSkills.libraries.isNotEmpty()) {
                    document.add(createSubsectionTitle("Библиотеки"))
                    val libTable = createSkillsTable(resume.professionalSkills.libraries)
                    document.add(libTable)
                }

                // Databases
                if (resume.professionalSkills.databases.isNotEmpty()) {
                    document.add(createSubsectionTitle("Базы данных"))
                    val dbTable = createSkillsTable(resume.professionalSkills.databases)
                    document.add(dbTable)
                }

                // Other Technologies
                if (resume.professionalSkills.otherTechnologies.isNotEmpty()) {
                    document.add(createSubsectionTitle("Другие технологии"))
                    val otherTable = createSkillsTable(resume.professionalSkills.otherTechnologies)
                    document.add(otherTable)
                }

                // Certifications
                if (resume.professionalSkills.certifications.isNotEmpty()) {
                    document.add(createSubsectionTitle(getRussianTitle("certifications")))
                    resume.professionalSkills.certifications.forEach { cert ->
                        document.add(createBulletPoint(cert))
                    }
                }

                // Soft Skills
                if (resume.professionalSkills.softSkills.isNotEmpty()) {
                    document.add(createSubsectionTitle(getRussianTitle("soft_skills")))
                    val softSkillsTable = createSkillsTable(resume.professionalSkills.softSkills)
                    document.add(softSkillsTable)
                }
            }

            // Professional Achievements
            if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
                document.add(createSectionTitle(getRussianTitle("achievements")))
                resume.professionalSkills.professionalAchievements.forEach { achievement ->
                    document.add(createAchievementItem(achievement))
                }
            }

            // Projects section — include description and responsibilities
            if (resume.projects.isNotEmpty()) {
                document.add(createSectionTitle(getRussianTitle("projects")))
                resume.projects.forEachIndexed { index, project ->
                    val projectCard = createProjectCard(project, index + 1)
                    document.add(projectCard)
                }
            }

            // Footer with generation info
            val footerDiv = Div()
                .setBackgroundColor(veryLightGray)
                .setPadding(10f)
                .setMarginTop(20f)
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
               resume.summary.isNotBlank() ||
               resume.personalInfo.education.isNotBlank() ||
               resume.personalInfo.languages.isNotEmpty() ||
               resume.professionalSkills.operatingSystems.isNotEmpty() ||
               resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
               resume.professionalSkills.certifications.isNotEmpty() ||
               resume.professionalSkills.softSkills.isNotEmpty() ||
               resume.professionalSkills.professionalAchievements.isNotEmpty() ||
               resume.projects.isNotEmpty()
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
        val textElement = Text(text).setFont(regularFont).setFontColor(darkGray)
        return Cell().setBorder(null)
            .setTextAlignment(TextAlignment.CENTER)
            .add(Paragraph(textElement).setFontSize(11f).setMargin(0f))
    }

    private fun createEmptyCell(): Cell {
        return Cell().setBorder(null)
    }

    private fun createDetailLabel(text: String): Cell {
        val textElement = Text(text).setFont(boldFont).setFontColor(lightGray)
        return Cell().setBorder(null)
            .add(Paragraph(textElement).setFontSize(11f).setMarginBottom(3f))
    }

    private fun createDetailValue(text: String): Cell {
        val textElement = Text(text).setFont(regularFont).setFontColor(primaryColor)
        return Cell().setBorder(null)
            .add(Paragraph(textElement).setFontSize(12f).setMarginBottom(0f))
    }

    private fun createLanguagesTable(languages: List<com.bober.autcsv.domain.model.Language>): Table {
        val table = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        languages.forEach { lang ->
            table.addCell(createDetailLabel(lang.name))
            table.addCell(createDetailValue(lang.level))
        }

        return table
    }

    private fun createSkillsTable(skills: List<String>): Table {
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

    private fun createProjectCard(project: com.bober.autcsv.domain.model.Project, projectNumber: Int): Div {
        val projectCard = Div()
            .setBackgroundColor(cardBackground)
            .setPadding(20f)
            .setMarginBottom(20f)

        // Project header with number
        projectCard.add(createSubheading("Проект $projectNumber: ${project.name}"))
        
        // Project details table
        val detailsTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)
            .setMarginBottom(15f)

        if (project.role.isNotBlank()) {
            detailsTable.addCell(createDetailLabel(getRussianTitle("role")))
            detailsTable.addCell(createDetailValue(project.role))
        }

        if (project.duration.isNotBlank()) {
            detailsTable.addCell(createDetailLabel(getRussianTitle("duration")))
            detailsTable.addCell(createDetailValue(project.duration))
        }

        if (project.teamSize.isNotBlank()) {
            detailsTable.addCell(createDetailLabel(getRussianTitle("team_size")))
            detailsTable.addCell(createDetailValue(project.teamSize))
        }

        if (project.technologies.isNotEmpty()) {
            detailsTable.addCell(createDetailLabel(getRussianTitle("technologies")))
            detailsTable.addCell(createDetailValue(project.technologies.joinToString(", ")))
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
} 