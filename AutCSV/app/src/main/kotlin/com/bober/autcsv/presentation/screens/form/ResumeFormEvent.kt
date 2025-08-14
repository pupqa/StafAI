package com.bober.autcsv.presentation.screens.form

sealed class ResumeFormEvent {
    data class FullNameChanged(val value: String) : ResumeFormEvent()
    data class SpecializationChanged(val value: String) : ResumeFormEvent()
    data class TotalExperienceChanged(val value: String) : ResumeFormEvent()
    data class SpecializationExperienceChanged(val value: String) : ResumeFormEvent()
    data class EducationChanged(val value: String) : ResumeFormEvent()
    data class EmailChanged(val value: String) : ResumeFormEvent()
    data class PhoneChanged(val value: String) : ResumeFormEvent()
    data class AboutMeChanged(val value: String) : ResumeFormEvent()
    data class LocationChanged(val value: String) : ResumeFormEvent()

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

    data class ProjectResponsibilityChanged(val index: Int, val respIndex: Int, val value: String) :
        ResumeFormEvent()

    data class DeleteProjectResponsibility(val index: Int, val respIndex: Int) : ResumeFormEvent()
    data class AddProjectResponsibility(val index: Int) : ResumeFormEvent()
    data class DeleteProject(val index: Int) : ResumeFormEvent()
    object AddProject : ResumeFormEvent()

    object Submit : ResumeFormEvent()
} 