package com.bober.autcsv.presentation.screens.form

/**
 * События формы резюме: изменения полей, управление списками, отправка формы.
 */
sealed class ResumeFormEvent {
    data class FullNameChanged(val value: String) : ResumeFormEvent()
    data class SpecializationChanged(val value: String) : ResumeFormEvent()
    data class TotalExperienceChanged(val value: String) : ResumeFormEvent()
    data class SpecializationExperienceChanged(val value: String) : ResumeFormEvent()
    data class EducationEntryLevelChanged(
        val index: Int,
        val value: SuggestionDictionary.EducationLevel,
    ) : ResumeFormEvent()

    data class EducationEntrySpecialtyChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class EducationEntryInstitutionChanged(val index: Int, val value: String) :
        ResumeFormEvent()

    data class DeleteEducationEntry(val index: Int) : ResumeFormEvent()
    object AddEducationEntry : ResumeFormEvent()
    data class EmailChanged(val value: String) : ResumeFormEvent()
    data class PhoneChanged(val value: String) : ResumeFormEvent()
    data class AboutMeChanged(val value: String) : ResumeFormEvent()
    data class LocationChanged(val value: String) : ResumeFormEvent()

    data class SalaryMinChanged(val value: String) : ResumeFormEvent()
    data class SalaryMaxChanged(val value: String) : ResumeFormEvent()
    data class RelocationChanged(val value: String) : ResumeFormEvent()
    data class RelocationCitiesChanged(val value: String) : ResumeFormEvent()
    data class EmploymentToggled(val value: String) : ResumeFormEvent()
    data class WorkScheduleToggled(val value: String) : ResumeFormEvent()
    data class PhotoChanged(val path: String) : ResumeFormEvent()

    data class SocialLinkPlatformChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class SocialLinkUrlChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class DeleteSocialLink(val index: Int) : ResumeFormEvent()
    object AddSocialLink : ResumeFormEvent()

    data class LanguageNameChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class LanguageLevelChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class DeleteLanguage(val index: Int) : ResumeFormEvent()
    object AddLanguage : ResumeFormEvent()

    data class OperatingSystemsChanged(val value: String) : ResumeFormEvent()
    data class ProfessionalAchievementChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class ProgrammingLanguagesChanged(val value: String) : ResumeFormEvent()
    data class FrameworksChanged(val value: String) : ResumeFormEvent()
    data class LibrariesChanged(val value: String) : ResumeFormEvent()
    data class DatabasesChanged(val value: String) : ResumeFormEvent()
    data class OtherTechnologiesChanged(val value: String) : ResumeFormEvent()
    data class CertificationsChanged(val value: String) : ResumeFormEvent()
    data class SoftSkillsChanged(val value: String) : ResumeFormEvent()
    data class DeleteProfessionalAchievement(val index: Int) : ResumeFormEvent()
    object AddProfessionalAchievement : ResumeFormEvent()

    data class ProjectNameChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class ProjectRoleChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class ProjectDurationChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class ProjectDescriptionChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class ProjectTechnologiesChanged(val index: Int, val value: String) :
        ResumeFormEvent()

    data class ProjectTeamSizeChanged(val index: Int, val value: String) : ResumeFormEvent()

    /** Задачи проекта одним многострочным текстом: строка = отдельный пункт. */
    data class ProjectResponsibilitiesChanged(val index: Int, val value: String) :
        ResumeFormEvent()

    data class ProjectLinkChanged(val index: Int, val value: String) : ResumeFormEvent()
    data class DeleteProject(val index: Int) : ResumeFormEvent()
    object AddProject : ResumeFormEvent()

    object Submit : ResumeFormEvent()

    /** LLM: переписать раздел «О себе» профессиональнее. */
    object ImproveAboutMe : ResumeFormEvent()

    /** LLM: сгенерировать «О себе» по заполненным данным. */
    object GenerateAboutMe : ResumeFormEvent()
} 