package com.bober.autcsv.domain.model

/**
 * Доменная модель резюме.
 * Объединяет персональные данные, навыки, проекты и агрегированную AI-оценку.
 */
data class Resume(
    val id: String = "",
    val personalInfo: PersonalInfo,
    val professionalSkills: ProfessionalSkills,
    val projects: List<Project>,
    val summary: String = "",
    val aiAnalysis: AiAnalysis = AiAnalysis(),
    val lastModified: Long = System.currentTimeMillis(),
) {
    /**
     * Базовая валидация: обязательные поля заполнены, проекты корректны.
     */
    fun isValid(): Boolean = personalInfo.isValid() &&
            professionalSkills.isValid() &&
            (projects.isEmpty() || projects.all { it.isValid() })
}

/**
 * Персональная информация кандидата.
 */
data class PersonalInfo(
    val fullName: String,
    val specialization: String,
    val totalExperience: String,
    val specializationExperience: String = "",
    val education: String = "",
    val languages: List<Language> = listOf(),
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val aboutMe: String = "",
) {
    /**
     * Валидация обязательных полей: ФИО, специализация и общий опыт.
     */
    fun isValid(): Boolean = fullName.isNotBlank() &&
            specialization.isNotBlank() &&
            totalExperience.isNotBlank()
}

/**
 * Язык и уровень владения.
 */
data class Language(
    val name: String,
    val level: String,
) {
    /**
     * Проверка что и язык, и уровень указаны.
     */
    fun isValid(): Boolean = name.isNotBlank() && level.isNotBlank()
}

/**
 * Профессиональные навыки и связанные списки.
 */
data class ProfessionalSkills(
    val operatingSystems: List<String> = emptyList(),
    val programmingLanguages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val otherTechnologies: List<String> = emptyList(),
    val professionalAchievements: List<String>,
    val certifications: List<String> = emptyList(),
    val softSkills: List<String> = emptyList(),
) {
    /**
     * Навыки валидны, если есть хотя бы один технический список.
     */
    fun isValid(): Boolean = programmingLanguages.isNotEmpty() || 
                             frameworks.isNotEmpty() || 
                             libraries.isNotEmpty() || 
                             databases.isNotEmpty() || 
                             otherTechnologies.isNotEmpty()
    
    /**
     * Собирает сводный список технологий для быстрой фильтрации/поиска.
     */
    fun getAllTechnologies(): List<String> = 
        programmingLanguages + frameworks + libraries + databases + otherTechnologies
}

/**
 * Описание проекта в резюме.
 */
data class Project(
    val name: String,
    val role: String,
    val duration: String,
    val description: String,
    val technologies: List<String> = emptyList(),
    val responsibilities: List<String> = emptyList(),
    val teamSize: String = "",
) {
    /**
     * Минимальная валидация обязательных атрибутов проекта.
     */
    fun isValid(): Boolean = name.isNotBlank() &&
            role.isNotBlank() &&
            duration.isNotBlank() &&
            description.isNotBlank()
}

/**
 * Результаты AI-анализа и производные улучшения.
 */
data class AiAnalysis(
    val completenessScore: Int = 0,
    val suggestedImprovements: List<String> = emptyList(),
    val strengthAreas: List<String> = emptyList(),
    val weakAreas: List<String> = emptyList(),
    val enhancedDescriptions: Map<String, String> = emptyMap(),
    val keywordSuggestions: List<String> = emptyList(),
    val lastAnalyzed: Long = 0,
) {
    /**
     * В текущей версии всегда true, оставлено для симметрии и дальнейшего расширения.
     */
    fun isValid(): Boolean = true
} 