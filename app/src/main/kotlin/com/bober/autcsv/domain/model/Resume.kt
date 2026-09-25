package com.bober.autcsv.domain.model

data class Resume(
    val id: String = "",
    val personalInfo: PersonalInfo,
    val professionalSkills: ProfessionalSkills,
    val projects: List<Project>,
    val summary: String = "",
    val aiAnalysis: AiAnalysis = AiAnalysis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    /** Статус кандидата в пайплайне рекрутера. */
    val status: CandidateStatus = CandidateStatus.NONE,
    /** Избранное (короткий лист). */
    val isFavorite: Boolean = false,
) {
    fun isValid(): Boolean = personalInfo.isValid() &&
            professionalSkills.isValid() &&
            (projects.isEmpty() || projects.all { it.isValid() })
}

data class PersonalInfo(
    val fullName: String,
    val specialization: String,
    val totalExperience: String,
    val specializationExperience: String = "",
    val education: String = "",
    /** Несколько образований кандидата; [education] — текстовое представление для экспорта. */
    val educations: List<EducationEntry> = emptyList(),
    val languages: List<Language> = listOf(),
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val aboutMe: String = "",
    /** Желаемая зарплата: нижняя граница вилки, сумма в рублях. */
    val salaryMin: String = "",
    /** Верхняя граница зарплатной вилки, сумма в рублях. */
    val salaryMax: String = "",
    /** Готовность к релокации: текстовая метка (см. SuggestionDictionary.RelocationOption). */
    val readyToRelocate: String = "",
    /** Города, в которые кандидат готов переехать, через запятую. */
    val relocationCities: String = "",
    /** Локальный путь к фото кандидата (файл в filesDir приложения). */
    val photoUri: String = "",
    /** Желаемая занятость (полная/частичная и т.д.), метки через запятую. */
    val employment: String = "",
    /** Желаемый график (удалёнка/гибрид/офис), метки через запятую. */
    val workSchedule: String = "",
    /** Ссылки на соцсети и профессиональные профили. */
    val socialLinks: List<SocialLink> = emptyList(),
) {
    fun isValid(): Boolean = fullName.isNotBlank() &&
            specialization.isNotBlank() &&
            totalExperience.isNotBlank() &&
            salaryMin.isNotBlank() &&
            readyToRelocate.isNotBlank()
}

/** Ссылка на соцсеть или профессиональный профиль (Telegram, LinkedIn, GitHub…). */
data class SocialLink(
    val platform: String = "",
    val url: String = "",
)

/**
 * Одна запись об образовании: уровень (label из SuggestionDictionary.EducationLevel),
 * специальность и учебное заведение. Резюме может содержать несколько записей.
 */
data class EducationEntry(
    val level: String = "",
    val specialty: String = "",
    val institution: String = "",
) {
    fun isBlankEntry(): Boolean = level.isBlank() && specialty.isBlank() && institution.isBlank()

    /** Строка для экспорта/PDF: «вуз, специальность, уровень» без пустых частей. */
    fun describe(): String = listOf(institution, specialty, level)
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

data class Language(
    val name: String,
    val level: String,
) {
    fun isValid(): Boolean = name.isNotBlank() && level.isNotBlank()
}

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
    fun isValid(): Boolean = programmingLanguages.isNotEmpty() ||
            frameworks.isNotEmpty() ||
            libraries.isNotEmpty() ||
            databases.isNotEmpty() ||
            otherTechnologies.isNotEmpty()

    fun getAllTechnologies(): List<String> =
        programmingLanguages + frameworks + libraries + databases + otherTechnologies
}

data class Project(
    val name: String,
    val role: String,
    val duration: String,
    val description: String,
    val technologies: List<String> = emptyList(),
    val responsibilities: List<String> = emptyList(),
    val teamSize: String = "",
    /** Ссылка на репозиторий/демо/публикацию; необязательное поле. */
    val link: String = "",
) {
    fun isValid(): Boolean = name.isNotBlank() &&
            role.isNotBlank() &&
            duration.isNotBlank() &&
            description.isNotBlank()
}

data class AiAnalysis(
    val completenessScore: Int = 0,
    val suggestedImprovements: List<String> = emptyList(),
    val strengthAreas: List<String> = emptyList(),
    val weakAreas: List<String> = emptyList(),
    val enhancedDescriptions: Map<String, String> = emptyMap(),
    val keywordSuggestions: List<String> = emptyList(),
    val lastAnalyzed: Long = 0,
) {
    fun isValid(): Boolean = true
} 