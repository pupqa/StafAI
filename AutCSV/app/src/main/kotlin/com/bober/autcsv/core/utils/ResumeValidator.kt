package com.bober.autcsv.core.utils

import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.core.utils.PhoneFormatter

object ResumeValidator {
    
    data class ValidationResult(
        val isValid: Boolean,
        val completenessScore: Int,
        val missingFields: List<String>,
        val suggestions: List<String>,
        val warnings: List<String>
    )

    fun validateResume(resume: Resume): ValidationResult {
        val missingFields = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var score = 0
        val totalFields = 15

        // Personal Information (40% weight)
        if (resume.personalInfo.fullName.isNotBlank()) {
            score += 2
        } else {
            missingFields.add("Полное имя")
        }

        if (resume.personalInfo.specialization.isNotBlank()) {
            score += 2
        } else {
            missingFields.add("Специализация")
        }

        if (resume.personalInfo.totalExperience.isNotBlank()) {
            score += 2
        } else {
            missingFields.add("Общий опыт работы")
        }

        if (resume.personalInfo.email.isNotBlank()) {
            score += 1
        } else {
            missingFields.add("Email")
            warnings.add("Email необходим для связи с работодателем")
        }

        if (resume.personalInfo.phone.isNotBlank() && PhoneFormatter.isValidPhoneNumber(resume.personalInfo.phone)) {
            score += 1
        } else {
            missingFields.add("Телефон")
            warnings.add("Телефон необходим для связи с работодателем")
        }

        if (resume.personalInfo.location.isNotBlank()) {
            score += 1
        } else {
            missingFields.add("Местоположение")
        }

        if (resume.personalInfo.education.isNotBlank()) {
            score += 1
        } else {
            missingFields.add("Образование")
            suggestions.add("Добавьте информацию об образовании")
        }

        // Professional Skills (40% weight)
        if (resume.professionalSkills.getAllTechnologies().isNotEmpty()) {
            score += 2
        } else {
            missingFields.add("Технические навыки")
            warnings.add("Технические навыки критически важны для IT-резюме")
        }

        if (resume.professionalSkills.professionalAchievements.isNotEmpty()) {
            score += 2
        } else {
            missingFields.add("Достижения")
            suggestions.add("Добавьте конкретные достижения с цифрами")
        }

        if (resume.professionalSkills.softSkills.isNotEmpty()) {
            score += 1
        } else {
            missingFields.add("Гибкие навыки")
            suggestions.add("Добавьте soft skills (коммуникация, лидерство и т.д.)")
        }

        if (resume.professionalSkills.certifications.isNotEmpty()) {
            score += 1
        } else {
            missingFields.add("Сертификации")
            suggestions.add("Добавьте профессиональные сертификации")
        }

        // Projects (20% weight)
        if (resume.projects.isNotEmpty()) {
            score += 2
        } else {
            missingFields.add("Проекты")
            warnings.add("Проектный опыт очень важен для IT-специалистов")
        }

        // Summary
        if (resume.summary.isNotBlank()) {
            score += 1
        } else {
            missingFields.add("Профессиональное резюме")
            suggestions.add("Добавьте краткое профессиональное резюме")
        }

        val completenessScore = (score * 100) / totalFields

        // Generate suggestions based on missing fields
        if (resume.projects.isEmpty()) {
            suggestions.add("Добавьте 2-3 ключевых проекта с описанием технологий и результатов")
        }

        if (resume.professionalSkills.getAllTechnologies().size < 5) {
            suggestions.add("Добавьте больше технических навыков (минимум 5-7)")
        }

        if (resume.professionalSkills.professionalAchievements.size < 3) {
            suggestions.add("Добавьте больше достижений с конкретными результатами")
        }

        if (resume.personalInfo.languages.isEmpty()) {
            suggestions.add("Добавьте языковые навыки")
        }

        return ValidationResult(
            isValid = completenessScore >= 60,
            completenessScore = completenessScore,
            missingFields = missingFields,
            suggestions = suggestions,
            warnings = warnings
        )
    }

    fun getResumeQualityLevel(resume: Resume): String {
        val result = validateResume(resume)
        return when {
            result.completenessScore >= 90 -> "Отличное"
            result.completenessScore >= 75 -> "Хорошее"
            result.completenessScore >= 60 -> "Удовлетворительное"
            result.completenessScore >= 40 -> "Требует доработки"
            else -> "Неполное"
        }
    }

    fun getPriorityFields(resume: Resume): List<String> {
        val result = validateResume(resume)
        val priorityFields = mutableListOf<String>()

        // Critical fields
        if (resume.personalInfo.fullName.isBlank()) {
            priorityFields.add("Полное имя")
        }
        if (resume.personalInfo.specialization.isBlank()) {
            priorityFields.add("Специализация")
        }
        if (resume.professionalSkills.getAllTechnologies().isEmpty()) {
            priorityFields.add("Технические навыки")
        }
        if (resume.projects.isEmpty()) {
            priorityFields.add("Проекты")
        }

        // Important fields
        if (resume.personalInfo.email.isBlank()) {
            priorityFields.add("Email")
        }
        if (resume.personalInfo.phone.isBlank() || !PhoneFormatter.isValidPhoneNumber(resume.personalInfo.phone)) {
            priorityFields.add("Телефон")
        }
        if (resume.professionalSkills.professionalAchievements.isEmpty()) {
            priorityFields.add("Достижения")
        }

        return priorityFields
    }

    fun generateResumeTips(resume: Resume): List<String> {
        val tips = mutableListOf<String>()

        if (resume.professionalSkills.getAllTechnologies().size < 5) {
            tips.add("💡 Добавьте больше технических навыков. Рекомендуется 5-10 ключевых технологий")
        }

        if (resume.projects.isEmpty()) {
            tips.add("💡 Проектный опыт критически важен. Добавьте 2-3 ключевых проекта с описанием")
        }

        if (resume.professionalSkills.professionalAchievements.isEmpty()) {
            tips.add("💡 Достижения выделяют вас среди других кандидатов. Добавьте конкретные результаты")
        }

        if (resume.summary.isBlank()) {
            tips.add("💡 Краткое резюме помогает работодателю быстро понять ваш профиль")
        }

        if (resume.personalInfo.languages.isEmpty()) {
            tips.add("💡 Языковые навыки важны для международных проектов")
        }

        if (resume.professionalSkills.certifications.isEmpty()) {
            tips.add("💡 Сертификации подтверждают ваши навыки и повышают доверие")
        }

        return tips
    }
} 