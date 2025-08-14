package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.Resume
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import java.io.File

class TestTemplate : PdfTemplate() {

    override fun generate(resume: Resume, outputFile: File): String {
        initializeFonts()
        val (pdf, document) = createDocument(outputFile)

        try {
            // Заголовок (без мусорных знаков)
            document.add(createHeading("Тестовый шаблон — проверка данных"))
            document.add(createBodyText("Дата: ${java.time.LocalDate.now()}"))
            document.add(createBodyText("Время: ${java.time.LocalTime.now()}").setMarginBottom(20f))

            // Личная информация
            document.add(createSectionTitle("Личная информация"))
            
            addField(document, "Полное имя", resume.personalInfo.fullName)
            addField(document, "Специализация", resume.personalInfo.specialization)
            addField(document, "Email", resume.personalInfo.email)
            addField(document, "Телефон", resume.personalInfo.phone)
            addField(document, "Местоположение", resume.personalInfo.location)
            addField(document, "Общий опыт работы", resume.personalInfo.totalExperience)
            addField(document, "Опыт в специализации", resume.personalInfo.specializationExperience)
            addField(document, "Образование", resume.personalInfo.education)
            addField(document, "О себе", resume.personalInfo.aboutMe)

            // Языки
            if (resume.personalInfo.languages.isNotEmpty()) {
                document.add(createSectionTitle("Языки"))
                resume.personalInfo.languages.forEach { lang ->
                    addField(document, lang.name, lang.level)
                }
            }

            // Профессиональное резюме
            if (resume.summary.isNotBlank()) {
                document.add(createSectionTitle("Профессиональное резюме"))
                document.add(createBodyText(resume.summary))
            }
            if (resume.personalInfo.aboutMe.isNotBlank()) {
                document.add(createSectionTitle("О себе"))
                document.add(createBodyText(resume.personalInfo.aboutMe))
            }

            // Технические навыки
            if (hasTechnicalSkills(resume)) {
                document.add(createSectionTitle("Технические навыки"))
                
                if (resume.professionalSkills.operatingSystems.isNotEmpty()) {
                    addField(document, "Операционные системы", resume.professionalSkills.operatingSystems.joinToString(", "))
                }
                
                if (resume.professionalSkills.programmingLanguages.isNotEmpty()) {
                    addField(document, "Языки программирования", resume.professionalSkills.programmingLanguages.joinToString(", "))
                }
                
                if (resume.professionalSkills.frameworks.isNotEmpty()) {
                    addField(document, "Фреймворки", resume.professionalSkills.frameworks.joinToString(", "))
                }
                
                if (resume.professionalSkills.libraries.isNotEmpty()) {
                    addField(document, "Библиотеки", resume.professionalSkills.libraries.joinToString(", "))
                }
                
                if (resume.professionalSkills.databases.isNotEmpty()) {
                    addField(document, "Базы данных", resume.professionalSkills.databases.joinToString(", "))
                }
                
                if (resume.professionalSkills.otherTechnologies.isNotEmpty()) {
                    addField(document, "Другие технологии", resume.professionalSkills.otherTechnologies.joinToString(", "))
                }
                
                if (resume.professionalSkills.certifications.isNotEmpty()) {
                    document.add(createSubsectionTitle("Сертификации:"))
                    resume.professionalSkills.certifications.forEach { cert ->
                        document.add(createBodyText("• $cert"))
                    }
                }
                
                if (resume.professionalSkills.softSkills.isNotEmpty()) {
                    addField(document, "Гибкие навыки", resume.professionalSkills.softSkills.joinToString(", "))
                }
            }

            // Ключевые достижения
            if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
                document.add(createSectionTitle("Ключевые достижения"))
                resume.professionalSkills.professionalAchievements.forEachIndexed { index, achievement ->
                    document.add(createBodyText("${index + 1}. $achievement"))
                }
            }

            // Проекты
            if (resume.projects.isNotEmpty()) {
                document.add(createSectionTitle("Проекты"))
                resume.projects.forEachIndexed { index, project ->
                    document.add(createSubsectionTitle("Проект ${index + 1}: ${project.name}"))
                    addField(document, "Роль", project.role)
                    addField(document, "Продолжительность", project.duration)
                    addField(document, "Размер команды", project.teamSize)
                    addField(document, "Технологии", project.technologies.joinToString(", "))
                    addField(document, "Описание", project.description)
                    
                    if (project.responsibilities.isNotEmpty()) {
                        document.add(createBodyText("Обязанности:"))
                        project.responsibilities.forEach { resp ->
                            document.add(createBodyText("• $resp"))
                        }
                    }
                    document.add(createBodyText("").setMarginBottom(10f))
                }
            }

            // Статистика заполнения
            document.add(createSectionTitle("Статистика заполнения"))
            val completeness = getResumeCompleteness(resume)
            document.add(createBodyText("Процент заполнения: $completeness%"))
            
            val totalFields = countTotalFields(resume)
            val filledFields = countFilledFields(resume)
            document.add(createBodyText("Заполнено полей: $filledFields из $totalFields"))

            // Футер
            document.add(createBodyText("").setMarginTop(20f))
            document.add(createBodyText("Конец тестового шаблона").setTextAlignment(TextAlignment.CENTER))

            document.close()
            return outputFile.absolutePath

        } catch (e: Exception) {
            pdf.close()
            throw e
        }
    }

    private fun addField(document: com.itextpdf.layout.Document, label: String, value: String) {
        if (value.isNotBlank()) {
            val fieldText = "$label: $value"
            document.add(createBodyText(fieldText))
        } else {
            val fieldText = "$label: [НЕ ЗАПОЛНЕНО]"
            val textElement = Text(fieldText).setFont(lightFont).setFontColor(lightGray)
            document.add(Paragraph(textElement).setFontSize(11f).setMarginBottom(6f))
        }
    }

    private fun hasTechnicalSkills(resume: Resume): Boolean {
        return resume.professionalSkills.operatingSystems.isNotEmpty() ||
               resume.professionalSkills.getAllTechnologies().isNotEmpty() ||
               resume.professionalSkills.certifications.isNotEmpty() ||
               resume.professionalSkills.softSkills.isNotEmpty()
    }

    private fun countTotalFields(resume: Resume): Int {
        var total = 9 // поля персональной информации
        total += resume.personalInfo.languages.size
        total += 6 // категории технических навыков
        total += resume.professionalSkills.certifications.size
        total += resume.professionalSkills.professionalAchievements.size
        total += resume.projects.size * 6 // поля проекта
        return total
    }

    private fun countFilledFields(resume: Resume): Int {
        var filled = 0
        
        // Персональная информация
        if (resume.personalInfo.fullName.isNotBlank()) filled++
        if (resume.personalInfo.specialization.isNotBlank()) filled++
        if (resume.personalInfo.email.isNotBlank()) filled++
        if (resume.personalInfo.phone.isNotBlank()) filled++
        if (resume.personalInfo.location.isNotBlank()) filled++
        if (resume.personalInfo.totalExperience.isNotBlank()) filled++
        if (resume.personalInfo.specializationExperience.isNotBlank()) filled++
        if (resume.personalInfo.education.isNotBlank()) filled++
        if (resume.personalInfo.aboutMe.isNotBlank()) filled++
        
        // Языки
        filled += resume.personalInfo.languages.size
        
        // Технические навыки
        if (resume.professionalSkills.operatingSystems.isNotEmpty()) filled++
        if (resume.professionalSkills.programmingLanguages.isNotEmpty()) filled++
        if (resume.professionalSkills.frameworks.isNotEmpty()) filled++
        if (resume.professionalSkills.libraries.isNotEmpty()) filled++
        if (resume.professionalSkills.databases.isNotEmpty()) filled++
        if (resume.professionalSkills.otherTechnologies.isNotEmpty()) filled++
        
        // Сертификации и достижения
        filled += resume.professionalSkills.certifications.size
        filled += resume.professionalSkills.professionalAchievements.size
        
        // Проекты
        resume.projects.forEach { project ->
            if (project.name.isNotBlank()) filled++
            if (project.role.isNotBlank()) filled++
            if (project.duration.isNotBlank()) filled++
            if (project.teamSize.isNotBlank()) filled++
            if (project.technologies.isNotEmpty()) filled++
            if (project.description.isNotBlank()) filled++
        }
        
        return filled
    }

    override fun hasMeaningfulContent(resume: Resume): Boolean {
        return true // Тестовый шаблон всегда показывает данные, даже если они пустые
    }
} 