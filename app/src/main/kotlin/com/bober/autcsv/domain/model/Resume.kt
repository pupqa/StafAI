package com.bober.autcsv.domain.model

data class Resume(
    val id: String = "",
    val personalInfo: PersonalInfo,
    val professionalSkills: ProfessionalSkills,
    val projects: List<Project>,
    val summary: String = "",
    val aiAnalysis: AiAnalysis = AiAnalysis(),
    val lastModified: Long = System.currentTimeMillis(),
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
    val languages: List<Language> = listOf(),
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val aboutMe: String = "",
) {
    fun isValid(): Boolean = fullName.isNotBlank() &&
            specialization.isNotBlank() &&
            totalExperience.isNotBlank()
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