package com.bober.autcsv.presentation.screens.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import com.bober.autcsv.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Временная структура для проектов в UI
data class ProjectFormState(
    val name: String = "",
    val role: String = "",
    val duration: String = "",
    val description: String = "",
    val technologies: String = "", // Храним как строку для UI
    val responsibilities: List<String> = listOf(),
    val teamSize: String = ""
)

data class ResumeFormState(
    val resumeId: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val specialization: String = "",
    val totalExperience: String = "",
    val specializationExperience: String = "",
    val education: String = "",
    val email: String = "",
    val phone: String = "",
    val aboutMe: String = "",
    val location: String = "",
    val languages: List<Language> = listOf(),
    val operatingSystems: String = "",
    val programmingLanguages: String = "",
    val frameworks: String = "",
    val libraries: String = "",
    val databases: String = "",
    val otherTechnologies: String = "",
    val certifications: String = "",
    val softSkills: String = "",
    val professionalAchievements: List<String> = listOf(),
    val projects: List<ProjectFormState> = listOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isValid: Boolean = false,
    val isDirty: Boolean = false,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class ResumeFormViewModel @Inject constructor(
    private val repository: ResumeRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ResumeFormState())
    val state = _state.asStateFlow()

    init {
        LlmLogger.logUiEvent("Form", "ViewModel initialized", "Resume ID: ${_state.value.resumeId}")
        savedStateHandle.get<String>("resumeId")?.let { resumeId ->
            LlmLogger.logUiEvent("Form", "Loading existing resume", "Resume ID: $resumeId")
            loadResume(resumeId)
        }
    }

    private fun loadResume(id: String) {
        LlmLogger.logDatabaseOperation("SELECT", "resume", id)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getResume(id)) {
                is Result.Success -> {
                    result.data?.let { resume ->
                        LlmLogger.logUiEvent("Form", "Resume loaded successfully", "Name: ${resume.personalInfo.fullName}")
                        _state.update { state ->
                            state.copy(
                                resumeId = resume.id,
                                fullName = resume.personalInfo.fullName,
                                specialization = resume.personalInfo.specialization,
                                totalExperience = resume.personalInfo.totalExperience,
                                specializationExperience = resume.personalInfo.specializationExperience,
                                education = resume.personalInfo.education,
                                email = resume.personalInfo.email,
                                phone = resume.personalInfo.phone,
                                aboutMe = resume.personalInfo.aboutMe,
                                location = resume.personalInfo.location,
                                languages = resume.personalInfo.languages,
                                operatingSystems = resume.professionalSkills.operatingSystems.joinToString(", "),
                                programmingLanguages = resume.professionalSkills.programmingLanguages.joinToString(", "),
                                frameworks = resume.professionalSkills.frameworks.joinToString(", "),
                                libraries = resume.professionalSkills.libraries.joinToString(", "),
                                databases = resume.professionalSkills.databases.joinToString(", "),
                                otherTechnologies = resume.professionalSkills.otherTechnologies.joinToString(", "),
                                certifications = resume.professionalSkills.certifications.joinToString(", "),
                                softSkills = resume.professionalSkills.softSkills.joinToString(", "),
                                professionalAchievements = resume.professionalSkills.professionalAchievements,
                                projects = resume.projects.map { project ->
                                    ProjectFormState(
                                        name = project.name,
                                        role = project.role,
                                        duration = project.duration,
                                        description = project.description,
                                        technologies = project.technologies.joinToString(", "),
                                        responsibilities = project.responsibilities,
                                        teamSize = project.teamSize
                                    )
                                },
                                isLoading = false,
                                isDirty = false
                            )
                        }
                        validateForm()
                    }
                }

                is Result.Error -> {
                    LlmLogger.logError("Не удалось загрузить резюме: ${result.exception.message}", result.exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Не удалось загрузить резюме"
                        )
                    }
                }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Load resume", duration)
        }
    }

    fun onEvent(event: ResumeFormEvent) {
        LlmLogger.logFormEvent(event.javaClass.simpleName, null, null)
        
        when (event) {
            is ResumeFormEvent.FullNameChanged -> {
                LlmLogger.logFormEvent("FullNameChanged", "fullName", event.value)
                updateState { it.copy(fullName = event.value) }
            }
            is ResumeFormEvent.SpecializationChanged -> {
                LlmLogger.logFormEvent("SpecializationChanged", "specialization", event.value)
                updateState { it.copy(specialization = event.value) }
            }
            is ResumeFormEvent.TotalExperienceChanged -> {
                LlmLogger.logFormEvent("TotalExperienceChanged", "totalExperience", event.value)
                updateState { it.copy(totalExperience = event.value) }
            }
            is ResumeFormEvent.SpecializationExperienceChanged -> {
                LlmLogger.logFormEvent("SpecializationExperienceChanged", "specializationExperience", event.value)
                updateState { it.copy(specializationExperience = event.value) }
            }
            is ResumeFormEvent.EducationChanged -> {
                LlmLogger.logFormEvent("EducationChanged", "education", event.value)
                updateState { it.copy(education = event.value) }
            }
            is ResumeFormEvent.EmailChanged -> {
                LlmLogger.logFormEvent("EmailChanged", "email", event.value)
                updateState { it.copy(email = event.value) }
            }
            is ResumeFormEvent.PhoneChanged -> {
                LlmLogger.logFormEvent("PhoneChanged", "phone", event.value)
                updateState { it.copy(phone = event.value) }
            }
            is ResumeFormEvent.AboutMeChanged -> {
                LlmLogger.logFormEvent("AboutMeChanged", "aboutMe", event.value)
                updateState { it.copy(aboutMe = event.value) }
            }
            is ResumeFormEvent.LocationChanged -> {
                LlmLogger.logFormEvent("LocationChanged", "location", event.value)
                updateState { it.copy(location = event.value) }
            }
            is ResumeFormEvent.SoftSkillsChanged -> {
                LlmLogger.logFormEvent("SoftSkillsChanged", "softSkills", event.value)
                updateState { it.copy(softSkills = event.value) }
            }
            is ResumeFormEvent.Submit -> {
                LlmLogger.logUserAction("Submit form", "Form", "Resume ID: ${state.value.resumeId}")
                submitForm()
            }
            is ResumeFormEvent.LanguageNameChanged,
            is ResumeFormEvent.LanguageLevelChanged,
            is ResumeFormEvent.AddLanguage,
            is ResumeFormEvent.DeleteLanguage,
            is ResumeFormEvent.OperatingSystemsChanged,
            is ResumeFormEvent.ProgrammingLanguagesChanged,
            is ResumeFormEvent.FrameworksChanged,
            is ResumeFormEvent.LibrariesChanged,
            is ResumeFormEvent.DatabasesChanged,
            is ResumeFormEvent.OtherTechnologiesChanged,
            is ResumeFormEvent.CertificationsChanged,
            is ResumeFormEvent.SoftSkillsChanged,
            is ResumeFormEvent.ProfessionalAchievementChanged,
            is ResumeFormEvent.AddProfessionalAchievement,
            is ResumeFormEvent.DeleteProfessionalAchievement,
            is ResumeFormEvent.ProjectNameChanged,
            is ResumeFormEvent.ProjectRoleChanged,
            is ResumeFormEvent.ProjectDurationChanged,
            is ResumeFormEvent.ProjectDescriptionChanged,
            is ResumeFormEvent.ProjectTechnologiesChanged,
            is ResumeFormEvent.ProjectTeamSizeChanged,
            is ResumeFormEvent.ProjectResponsibilityChanged,
            is ResumeFormEvent.DeleteProjectResponsibility,
            is ResumeFormEvent.AddProjectResponsibility,
            is ResumeFormEvent.DeleteProject,
            is ResumeFormEvent.AddProject,
                -> handleOtherEvents(event)

            else -> {}
        }
    }

    private fun handleOtherEvents(event: ResumeFormEvent) {
        when (event) {
            is ResumeFormEvent.LanguageNameChanged -> {
                LlmLogger.logFormEvent("LanguageNameChanged", "language[${event.index}].name", event.value)
                val updatedLanguages = state.value.languages.toMutableList()
                if (event.index < updatedLanguages.size) {
                    updatedLanguages[event.index] =
                        updatedLanguages[event.index].copy(name = event.value)
                    updateState { it.copy(languages = updatedLanguages) }
                }
            }

            is ResumeFormEvent.LanguageLevelChanged -> {
                LlmLogger.logFormEvent("LanguageLevelChanged", "language[${event.index}].level", event.value)
                val updatedLanguages = state.value.languages.toMutableList()
                if (event.index < updatedLanguages.size) {
                    updatedLanguages[event.index] =
                        updatedLanguages[event.index].copy(level = event.value)
                    updateState { it.copy(languages = updatedLanguages) }
                }
            }

            is ResumeFormEvent.AddLanguage -> {
                LlmLogger.logUserAction("Add language", "Form", null)
                updateState { it.copy(languages = it.languages + Language("", "")) }
            }

            is ResumeFormEvent.DeleteLanguage -> {
                LlmLogger.logUserAction("Delete language", "Form", "index: ${event.index}")
                updateState { it.copy(languages = it.languages.filterIndexed { index, _ -> index != event.index }) }
            }

            is ResumeFormEvent.OperatingSystemsChanged -> {
                LlmLogger.logFormEvent("OperatingSystemsChanged", "operatingSystems", event.value)
                updateState { it.copy(operatingSystems = event.value) }
            }
            is ResumeFormEvent.ProgrammingLanguagesChanged -> {
                LlmLogger.logFormEvent("ProgrammingLanguagesChanged", "programmingLanguages", event.value)
                updateState { it.copy(programmingLanguages = event.value) }
            }
            is ResumeFormEvent.FrameworksChanged -> {
                LlmLogger.logFormEvent("FrameworksChanged", "frameworks", event.value)
                updateState { it.copy(frameworks = event.value) }
            }
            is ResumeFormEvent.LibrariesChanged -> {
                LlmLogger.logFormEvent("LibrariesChanged", "libraries", event.value)
                updateState { it.copy(libraries = event.value) }
            }
            is ResumeFormEvent.DatabasesChanged -> {
                LlmLogger.logFormEvent("DatabasesChanged", "databases", event.value)
                updateState { it.copy(databases = event.value) }
            }
            is ResumeFormEvent.OtherTechnologiesChanged -> {
                LlmLogger.logFormEvent("OtherTechnologiesChanged", "otherTechnologies", event.value)
                updateState { it.copy(otherTechnologies = event.value) }
            }
            is ResumeFormEvent.CertificationsChanged -> {
                LlmLogger.logFormEvent("CertificationsChanged", "certifications", event.value)
                updateState { it.copy(certifications = event.value) }
            }
            is ResumeFormEvent.SoftSkillsChanged -> {
                LlmLogger.logFormEvent("SoftSkillsChanged", "softSkills", event.value)
                updateState { it.copy(softSkills = event.value) }
            }

            is ResumeFormEvent.ProfessionalAchievementChanged -> {
                LlmLogger.logFormEvent("ProfessionalAchievementChanged", "achievement[${event.index}]", event.value)
                val updatedAchievements = state.value.professionalAchievements.toMutableList()
                if (event.index < updatedAchievements.size) {
                    updatedAchievements[event.index] = event.value
                    updateState { it.copy(professionalAchievements = updatedAchievements) }
                }
            }

            is ResumeFormEvent.AddProfessionalAchievement -> {
                LlmLogger.logUserAction("Add achievement", "Form", null)
                updateState { it.copy(professionalAchievements = it.professionalAchievements + "") }
            }

            is ResumeFormEvent.DeleteProfessionalAchievement -> {
                LlmLogger.logUserAction("Delete achievement", "Form", "index: ${event.index}")
                updateState { it.copy(professionalAchievements = it.professionalAchievements.filterIndexed { index, _ -> index != event.index }) }
            }

            is ResumeFormEvent.ProjectNameChanged -> {
                LlmLogger.logFormEvent("ProjectNameChanged", "project[${event.index}].name", event.value)
                updateProject(event.index) { it.copy(name = event.value) }
            }

            is ResumeFormEvent.ProjectRoleChanged -> {
                LlmLogger.logFormEvent("ProjectRoleChanged", "project[${event.index}].role", event.value)
                updateProject(event.index) { it.copy(role = event.value) }
            }

            is ResumeFormEvent.ProjectDurationChanged -> {
                LlmLogger.logFormEvent("ProjectDurationChanged", "project[${event.index}].duration", event.value)
                updateProject(event.index) { it.copy(duration = event.value) }
            }

            is ResumeFormEvent.ProjectDescriptionChanged -> {
                LlmLogger.logFormEvent("ProjectDescriptionChanged", "project[${event.index}].description", event.value)
                updateProject(event.index) { it.copy(description = event.value) }
            }

            is ResumeFormEvent.ProjectTechnologiesChanged -> {
                LlmLogger.logFormEvent("ProjectTechnologiesChanged", "project[${event.index}].technologies", event.value)
                updateProject(event.index) { it.copy(technologies = event.value) }
            }

            is ResumeFormEvent.ProjectTeamSizeChanged -> {
                LlmLogger.logFormEvent("ProjectTeamSizeChanged", "project[${event.index}].teamSize", event.value)
                updateProject(event.index) { it.copy(teamSize = event.value) }
            }

            is ResumeFormEvent.ProjectResponsibilityChanged -> {
                LlmLogger.logFormEvent("ProjectResponsibilityChanged", "project[${event.index}].responsibility[${event.respIndex}]", event.value)
                val project = state.value.projects.getOrNull(event.index)
                project?.let {
                    val responsibilities = it.responsibilities.toMutableList()
                    if (event.respIndex < responsibilities.size) {
                        responsibilities[event.respIndex] = event.value
                        updateProject(event.index) { it.copy(responsibilities = responsibilities) }
                    }
                }
            }

            is ResumeFormEvent.DeleteProjectResponsibility -> {
                LlmLogger.logUserAction("Delete project responsibility", "Form", "project: ${event.index}, resp: ${event.respIndex}")
                val project = state.value.projects.getOrNull(event.index)
                project?.let {
                    val responsibilities =
                        it.responsibilities.filterIndexed { index, _ -> index != event.respIndex }
                    updateProject(event.index) { it.copy(responsibilities = responsibilities) }
                }
            }

            is ResumeFormEvent.AddProjectResponsibility -> {
                LlmLogger.logUserAction("Add project responsibility", "Form", "project: ${event.index}")
                val project = state.value.projects.getOrNull(event.index)
                project?.let {
                    val responsibilities = it.responsibilities + ""
                    updateProject(event.index) { it.copy(responsibilities = responsibilities) }
                }
            }

            is ResumeFormEvent.DeleteProject -> {
                LlmLogger.logUserAction("Delete project", "Form", "index: ${event.index}")
                updateState { it.copy(projects = it.projects.filterIndexed { index, _ -> index != event.index }) }
            }

            is ResumeFormEvent.AddProject -> {
                LlmLogger.logUserAction("Add project", "Form", null)
                val newProject = ProjectFormState()
                updateState { it.copy(projects = it.projects + newProject) }
            }

            else -> {} // Handle other events if needed
        }
    }

    private fun updateProject(index: Int, update: (ProjectFormState) -> ProjectFormState) {
        val updatedProjects = state.value.projects.toMutableList()
        if (index < updatedProjects.size) {
            updatedProjects[index] = update(updatedProjects[index])
            updateState { it.copy(projects = updatedProjects) }
        }
    }

    private fun updateState(update: (ResumeFormState) -> ResumeFormState) {
        _state.update {
            update(it).copy(isDirty = true).also { _ ->
                validateForm()
            }
        }
    }

    private fun parseTechnologies(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun validateForm() {
        _state.update { state ->
            val resume = Resume(
                id = state.resumeId,
                personalInfo = PersonalInfo(
                    fullName = state.fullName,
                    specialization = state.specialization,
                    totalExperience = state.totalExperience,
                    specializationExperience = state.specializationExperience,
                    education = state.education,
                    aboutMe = state.aboutMe,
                    location = state.location,
                    languages = state.languages,
                    email = state.email,
                    phone = state.phone
                ),
                professionalSkills = ProfessionalSkills(
                    operatingSystems = parseTechnologies(state.operatingSystems),
                    programmingLanguages = parseTechnologies(state.programmingLanguages),
                    frameworks = parseTechnologies(state.frameworks),
                    libraries = parseTechnologies(state.libraries),
                    databases = parseTechnologies(state.databases),
                    otherTechnologies = parseTechnologies(state.otherTechnologies),
                    certifications = parseTechnologies(state.certifications),
                    softSkills = parseTechnologies(state.softSkills),
                    professionalAchievements = state.professionalAchievements
                ),
                projects = state.projects.map { projectForm ->
                    Project(
                        name = projectForm.name,
                        role = projectForm.role,
                        duration = projectForm.duration,
                        description = projectForm.description,
                        technologies = parseTechnologies(projectForm.technologies),
                        responsibilities = projectForm.responsibilities,
                        teamSize = projectForm.teamSize
                    )
                }
            )
            val isValid = resume.isValid()
            LlmLogger.logUiEvent("Form", "Validation result", "Is valid: $isValid")
            state.copy(isValid = isValid)
        }
    }

    private fun submitForm() {
        if (!state.value.isValid) {
            LlmLogger.logWarning("Form submission attempted but form is not valid")
            return
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.update { it.copy(isLoading = true, error = null) }

            val resume = Resume(
                id = state.value.resumeId,
                personalInfo = PersonalInfo(
                    fullName = state.value.fullName,
                    specialization = state.value.specialization,
                    totalExperience = state.value.totalExperience,
                    specializationExperience = state.value.specializationExperience,
                    education = state.value.education,
                    aboutMe = state.value.aboutMe,
                    location = state.value.location,
                    languages = state.value.languages,
                    email = state.value.email,
                    phone = state.value.phone
                ),
                professionalSkills = ProfessionalSkills(
                    operatingSystems = parseTechnologies(state.value.operatingSystems),
                    programmingLanguages = parseTechnologies(state.value.programmingLanguages),
                    frameworks = parseTechnologies(state.value.frameworks),
                    libraries = parseTechnologies(state.value.libraries),
                    databases = parseTechnologies(state.value.databases),
                    otherTechnologies = parseTechnologies(state.value.otherTechnologies),
                    certifications = parseTechnologies(state.value.certifications),
                    softSkills = parseTechnologies(state.value.softSkills),
                    professionalAchievements = state.value.professionalAchievements
                ),
                projects = state.value.projects.map { projectForm ->
                    Project(
                        name = projectForm.name,
                        role = projectForm.role,
                        duration = projectForm.duration,
                        description = projectForm.description,
                        technologies = parseTechnologies(projectForm.technologies),
                        responsibilities = projectForm.responsibilities,
                        teamSize = projectForm.teamSize
                    )
                }
            )

            try {
                LlmLogger.logDatabaseOperation("INSERT/UPDATE", "resume", resume.id)
                repository.saveResume(resume)
                LlmLogger.logUiEvent("Form", "Resume saved successfully", "Resume ID: ${resume.id}")
                _state.update { it.copy(isLoading = false, error = null, isDirty = false, saveSuccess = true) }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось сохранить резюме: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save resume"
                    )
                }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Save resume", duration)
        }
    }

    fun consumeSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
} 