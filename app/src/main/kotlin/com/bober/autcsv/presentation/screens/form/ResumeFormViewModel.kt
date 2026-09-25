package com.bober.autcsv.presentation.screens.form

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.data.repository.Result
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.SocialLink
import com.bober.autcsv.domain.repository.ResumeRepository
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary.EducationLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
    val technologies: String = "", // Храним как строку для UI (теги через запятую)
    /** Задачи/достижения редактируются одним многострочным полем: строка = пункт. */
    val responsibilitiesText: String = "",
    val teamSize: String = "",
    /** Ссылка на репозиторий/демо; необязательное, валидируется как ссылка профиля. */
    val link: String = "",
)

/** Статус автосохранения черновика для индикатора в UI. */
enum class AutosaveStatus { IDLE, SAVING, SAVED }

data class ResumeFormState(
    val resumeId: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val specialization: String = "",
    val totalExperience: String = "",
    val specializationExperience: String = "",
    /** Образования: уровень + специальность + заведение, записей может быть несколько. */
    val educations: List<EducationEntry> = emptyList(),
    val email: String = "",
    val phone: String = "",
    val aboutMe: String = "",
    val location: String = "",
    val salaryMin: String = "",
    val salaryMax: String = "",
    val readyToRelocate: String = "",
    val relocationCities: String = "",
    val employment: String = "",
    val workSchedule: String = "",
    val photoUri: String = "",
    val socialLinks: List<SocialLink> = emptyList(),
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
    val progressPercent: Int = 0,
    val autosaveStatus: AutosaveStatus = AutosaveStatus.IDLE,
    /** Идёт LLM-операция (улучшение/генерация текста). */
    val aiBusy: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
/**
 * ViewModel формы резюме: управляет состоянием формы, валидацией (дебаунс),
 * автосохранением черновиков, загрузкой существующего резюме и сохранением.
 *
 * Производительность: валидация и пересчёт прогресса выполняются не на каждый
 * ввод, а по дебаунсу (300 мс); сборка доменной модели вынесена в единственную
 * функцию [buildResume] и переиспользуется валидацией и сохранением.
 */
class ResumeFormViewModel @Inject constructor(
    private val repository: ResumeRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ResumeFormState())
    val state = _state.asStateFlow()

    init {
        LlmLogger.logUiEvent("Form", "ViewModel initialized", "Resume ID: ${_state.value.resumeId}")

        // Дебаунс-валидация: тяжёлая пересборка + проверка не чаще раза в 300 мс
        viewModelScope.launch {
            _state.debounce(VALIDATION_DEBOUNCE_MS).distinctUntilChangedByFormFields().collect {
                revalidate()
            }
        }

        // Автосохранение черновика: через 1.5 с после последнего изменения
        viewModelScope.launch {
            _state.debounce(AUTOSAVE_DEBOUNCE_MS).collect { snapshot ->
                if (snapshot.isDirty && !snapshot.isLoading && snapshot.saveSuccess.not()) {
                    autosaveDraft()
                }
            }
        }

        savedStateHandle.get<String>("resumeId")?.let { resumeId ->
            LlmLogger.logUiEvent("Form", "Loading existing resume", "Resume ID: $resumeId")
            loadResume(resumeId)
        }
    }

    /** Загружает резюме для редактирования и инициализирует состояние формы. */
    private fun loadResume(id: String) {
        LlmLogger.logDatabaseOperation("SELECT", "resume", id)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getResume(id)) {
                is Result.Success -> {
                    result.data?.let { resume ->
                        LlmLogger.logUiEvent(
                            "Form",
                            "Resume loaded successfully",
                            "Name: ${resume.personalInfo.fullName}"
                        )
                        // Новые резюме хранят список образований; старые — одну строку
                        // «вуз, специальность, уровень», которую разбираем в одну запись
                        val loadedEducations = resume.personalInfo.educations.ifEmpty {
                            parseLegacyEducation(resume.personalInfo.education)?.let { legacy ->
                                listOf(
                                    EducationEntry(
                                        // Подпись уровня — на текущем языке интерфейса
                                        level = legacy.first
                                            ?.let { appContext.localizedString(it.labelRes) }
                                            .orEmpty(),
                                        specialty = legacy.second,
                                        institution = legacy.third
                                    )
                                )
                            }.orEmpty()
                        }
                        _state.update { state ->
                            state.copy(
                                resumeId = resume.id,
                                fullName = resume.personalInfo.fullName,
                                specialization = resume.personalInfo.specialization,
                                totalExperience = resume.personalInfo.totalExperience,
                                specializationExperience = resume.personalInfo.specializationExperience,
                                educations = loadedEducations,
                                email = resume.personalInfo.email,
                                phone = resume.personalInfo.phone,
                                aboutMe = resume.personalInfo.aboutMe,
                                location = resume.personalInfo.location,
                                salaryMin = resume.personalInfo.salaryMin,
                                salaryMax = resume.personalInfo.salaryMax,
                                readyToRelocate = resume.personalInfo.readyToRelocate,
                                relocationCities = resume.personalInfo.relocationCities,
                                employment = resume.personalInfo.employment,
                                workSchedule = resume.personalInfo.workSchedule,
                                photoUri = resume.personalInfo.photoUri,
                                socialLinks = resume.personalInfo.socialLinks,
                                languages = resume.personalInfo.languages,
                                operatingSystems = resume.professionalSkills.operatingSystems.joinToString(
                                    ", "
                                ),
                                programmingLanguages = resume.professionalSkills.programmingLanguages.joinToString(
                                    ", "
                                ),
                                frameworks = resume.professionalSkills.frameworks.joinToString(", "),
                                libraries = resume.professionalSkills.libraries.joinToString(", "),
                                databases = resume.professionalSkills.databases.joinToString(", "),
                                otherTechnologies = resume.professionalSkills.otherTechnologies.joinToString(
                                    ", "
                                ),
                                certifications = resume.professionalSkills.certifications.joinToString(
                                    ", "
                                ),
                                softSkills = resume.professionalSkills.softSkills.joinToString(", "),
                                professionalAchievements = resume.professionalSkills.professionalAchievements,
                                projects = resume.projects.map { project ->
                                    ProjectFormState(
                                        name = project.name,
                                        role = project.role,
                                        duration = project.duration,
                                        description = project.description,
                                        technologies = project.technologies.joinToString(", "),
                                        responsibilitiesText =
                                            project.responsibilities.joinToString("\n"),
                                        teamSize = project.teamSize,
                                        link = project.link
                                    )
                                },
                                isLoading = false,
                                isDirty = false
                            )
                        }
                        revalidate()
                    }
                }

                is Result.Error -> {
                    LlmLogger.logError(
                        "Не удалось загрузить резюме: ${result.exception.message}",
                        result.exception
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message
                                ?: appContext.localizedString(R.string.error_loading)
                        )
                    }
                }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Load resume", duration)
        }
    }

    /** Обработка всех событий формы. */
    fun onEvent(event: ResumeFormEvent) {
        LlmLogger.logFormEvent(event.javaClass.simpleName, null, null)

        when (event) {
            is ResumeFormEvent.FullNameChanged ->
                updateState { it.copy(fullName = event.value) }

            is ResumeFormEvent.SpecializationChanged ->
                updateState { it.copy(specialization = event.value) }

            is ResumeFormEvent.TotalExperienceChanged ->
                updateState { it.copy(totalExperience = event.value) }

            is ResumeFormEvent.SpecializationExperienceChanged ->
                updateState { it.copy(specializationExperience = event.value) }

            is ResumeFormEvent.EducationEntryLevelChanged ->
                updateEducationEntry(event.index) { entry ->
                    entry.copy(
                        // Храним подпись текущей локали: это значение попадает в PDF/DOCX
                        level = appContext.localizedString(event.value.labelRes),
                        // Специальность предыдущего уровня больше не релевантна
                        specialty = if (SuggestionDictionary.specialtiesFor(appContext, event.value)
                                .contains(entry.specialty)
                        ) entry.specialty else ""
                    )
                }

            is ResumeFormEvent.EducationEntrySpecialtyChanged ->
                updateEducationEntry(event.index) { it.copy(specialty = event.value) }

            is ResumeFormEvent.EducationEntryInstitutionChanged ->
                updateEducationEntry(event.index) { it.copy(institution = event.value) }

            is ResumeFormEvent.AddEducationEntry ->
                updateState { it.copy(educations = it.educations + EducationEntry()) }

            is ResumeFormEvent.DeleteEducationEntry ->
                updateState {
                    it.copy(educations = it.educations.filterIndexed { index, _ -> index != event.index })
                }

            is ResumeFormEvent.EmailChanged ->
                updateState { it.copy(email = event.value) }

            is ResumeFormEvent.PhoneChanged ->
                updateState { it.copy(phone = event.value) }

            is ResumeFormEvent.AboutMeChanged ->
                updateState { it.copy(aboutMe = event.value) }

            is ResumeFormEvent.LocationChanged ->
                updateState { it.copy(location = event.value) }

            is ResumeFormEvent.SalaryMinChanged ->
                updateState { it.copy(salaryMin = event.value) }

            is ResumeFormEvent.SalaryMaxChanged ->
                updateState { it.copy(salaryMax = event.value) }

            is ResumeFormEvent.RelocationChanged ->
                updateState { it.copy(readyToRelocate = event.value) }

            is ResumeFormEvent.RelocationCitiesChanged ->
                updateState { it.copy(relocationCities = event.value) }

            is ResumeFormEvent.EmploymentToggled ->
                updateState { it.copy(employment = toggleListItem(it.employment, event.value)) }

            is ResumeFormEvent.WorkScheduleToggled ->
                updateState { it.copy(workSchedule = toggleListItem(it.workSchedule, event.value)) }

            is ResumeFormEvent.PhotoChanged ->
                updateState { it.copy(photoUri = event.path) }

            is ResumeFormEvent.SocialLinkPlatformChanged ->
                updateSocialLink(event.index) { it.copy(platform = event.value) }

            is ResumeFormEvent.SocialLinkUrlChanged ->
                updateSocialLink(event.index) { it.copy(url = event.value) }

            is ResumeFormEvent.AddSocialLink ->
                updateState { it.copy(socialLinks = it.socialLinks + SocialLink()) }

            is ResumeFormEvent.DeleteSocialLink ->
                updateState {
                    it.copy(socialLinks = it.socialLinks.filterIndexed { index, _ -> index != event.index })
                }

            is ResumeFormEvent.SoftSkillsChanged ->
                updateState { it.copy(softSkills = event.value) }

            is ResumeFormEvent.LanguageNameChanged ->
                updateState {
                    it.copy(languages = it.languages.mapIndexed { index, lang ->
                        if (index == event.index) lang.copy(name = event.value) else lang
                    })
                }

            is ResumeFormEvent.LanguageLevelChanged ->
                updateState {
                    it.copy(languages = it.languages.mapIndexed { index, lang ->
                        if (index == event.index) lang.copy(level = event.value) else lang
                    })
                }

            is ResumeFormEvent.AddLanguage ->
                updateState { it.copy(languages = it.languages + Language("", "")) }

            is ResumeFormEvent.DeleteLanguage ->
                updateState { it.copy(languages = it.languages.filterIndexed { index, _ -> index != event.index }) }

            is ResumeFormEvent.OperatingSystemsChanged ->
                updateState { it.copy(operatingSystems = event.value) }

            is ResumeFormEvent.ProgrammingLanguagesChanged ->
                updateState { it.copy(programmingLanguages = event.value) }

            is ResumeFormEvent.FrameworksChanged ->
                updateState { it.copy(frameworks = event.value) }

            is ResumeFormEvent.LibrariesChanged ->
                updateState { it.copy(libraries = event.value) }

            is ResumeFormEvent.DatabasesChanged ->
                updateState { it.copy(databases = event.value) }

            is ResumeFormEvent.OtherTechnologiesChanged ->
                updateState { it.copy(otherTechnologies = event.value) }

            is ResumeFormEvent.CertificationsChanged ->
                updateState { it.copy(certifications = event.value) }

            is ResumeFormEvent.ProfessionalAchievementChanged ->
                updateState {
                    it.copy(
                        professionalAchievements = it.professionalAchievements.toMutableList()
                            .also { list ->
                                if (event.index < list.size) list[event.index] = event.value
                            })
                }

            is ResumeFormEvent.AddProfessionalAchievement ->
                updateState { it.copy(professionalAchievements = it.professionalAchievements + "") }

            is ResumeFormEvent.DeleteProfessionalAchievement ->
                updateState {
                    it.copy(
                        professionalAchievements = it.professionalAchievements
                            .filterIndexed { index, _ -> index != event.index }
                    )
                }

            is ResumeFormEvent.ProjectNameChanged ->
                updateProject(event.index) { it.copy(name = event.value) }

            is ResumeFormEvent.ProjectRoleChanged ->
                updateProject(event.index) { it.copy(role = event.value) }

            is ResumeFormEvent.ProjectDurationChanged ->
                updateProject(event.index) { it.copy(duration = event.value) }

            is ResumeFormEvent.ProjectDescriptionChanged ->
                updateProject(event.index) { it.copy(description = event.value) }

            is ResumeFormEvent.ProjectTechnologiesChanged ->
                updateProject(event.index) { it.copy(technologies = event.value) }

            is ResumeFormEvent.ProjectTeamSizeChanged ->
                updateProject(event.index) { it.copy(teamSize = event.value) }

            is ResumeFormEvent.ProjectResponsibilitiesChanged ->
                updateProject(event.index) { it.copy(responsibilitiesText = event.value) }

            is ResumeFormEvent.ProjectLinkChanged ->
                updateProject(event.index) { it.copy(link = event.value) }

            is ResumeFormEvent.DeleteProject ->
                updateState { it.copy(projects = it.projects.filterIndexed { index, _ -> index != event.index }) }

            is ResumeFormEvent.AddProject ->
                updateState { it.copy(projects = it.projects + ProjectFormState()) }

            is ResumeFormEvent.Submit -> {
                LlmLogger.logUserAction("Submit form", "Form", "Resume ID: ${state.value.resumeId}")
                submitForm()
            }

            is ResumeFormEvent.ImproveAboutMe ->
                improveAboutMe()

            is ResumeFormEvent.GenerateAboutMe ->
                generateAboutMe()
        }
    }

    /** LLM: переписать «О себе» профессиональнее. */
    private fun improveAboutMe() {
        val snapshot = _state.value
        if (snapshot.aiBusy || snapshot.aboutMe.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(aiBusy = true, error = null) }
            try {
                val improved = repository.improveText(
                    snapshot.aboutMe,
                    com.bober.autcsv.data.api.llm.OpenRouterService.TextKind.ABOUT_ME
                )
                if (improved.isNotBlank()) {
                    updateState { it.copy(aboutMe = improved) }
                }
            } catch (e: Exception) {
                LlmLogger.logError("ИИ-улучшение «О себе» не удалось", e)
                _state.update {
                    it.copy(
                        error = e.message
                            ?: appContext.localizedString(R.string.error_improve_failed)
                    )
                }
            } finally {
                _state.update { it.copy(aiBusy = false) }
            }
        }
    }

    /** LLM: сгенерировать «О себе» по данным резюме. */
    private fun generateAboutMe() {
        val snapshot = _state.value
        if (snapshot.aiBusy) return
        viewModelScope.launch {
            _state.update { it.copy(aiBusy = true, error = null) }
            try {
                val generated = repository.generateAboutMe(buildResume(snapshot))
                if (generated.isNotBlank()) {
                    updateState { it.copy(aboutMe = generated) }
                }
            } catch (e: Exception) {
                LlmLogger.logError("ИИ-генерация «О себе» не удалось", e)
                _state.update {
                    it.copy(
                        error = e.message
                            ?: appContext.localizedString(R.string.error_generate_failed)
                    )
                }
            } finally {
                _state.update { it.copy(aiBusy = false) }
            }
        }
    }

    /** Обновляет одну запись об образовании в состоянии по индексу. */
    private fun updateEducationEntry(index: Int, update: (EducationEntry) -> EducationEntry) {
        updateState { state ->
            if (index >= state.educations.size) state
            else state.copy(
                educations = state.educations.toMutableList()
                    .also { it[index] = update(it[index]) }
            )
        }
    }

    /** Обновляет один проект в состоянии по индексу. */
    private fun updateProject(index: Int, update: (ProjectFormState) -> ProjectFormState) {
        updateState { state ->
            if (index >= state.projects.size) state
            else state.copy(
                projects = state.projects.toMutableList()
                    .also { it[index] = update(it[index]) }
            )
        }
    }

    /** Обновляет одну ссылку на соцсеть в состоянии по индексу. */
    private fun updateSocialLink(index: Int, update: (SocialLink) -> SocialLink) {
        updateState { state ->
            if (index >= state.socialLinks.size) state
            else state.copy(
                socialLinks = state.socialLinks.toMutableList()
                    .also { it[index] = update(it[index]) }
            )
        }
    }

    /**
     * Обновляет состояние и помечает форму изменённой.
     * Валидация запускается отдельно по дебаунсу — см. init.
     */
    private fun updateState(update: (ResumeFormState) -> ResumeFormState) {
        _state.update { update(it).copy(isDirty = true) }
    }

    /** Переключает элемент в строке-списке «a, b, c» (мультивыбор чипами). */
    private fun toggleListItem(current: String, item: String): String {
        val items = current.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val updated = if (items.any { it.equals(item, ignoreCase = true) }) {
            items.filterNot { it.equals(item, ignoreCase = true) }
        } else {
            items + item
        }
        return updated.joinToString(", ")
    }

    /** Парсит строку со списком технологий, разделённых запятыми,
     * и возвращает нормализованный список.
     */
    private fun parseTechnologies(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotBlank() }

    /**
     * Многострочное поле задач → список пунктов: пустые строки и пробелы
     * по краям отбрасываются, чтобы случайные переводы строк не стали буллетами.
     */
    private fun parseResponsibilityLines(input: String): List<String> =
        input.lines().map { it.trim() }.filter { it.isNotBlank() }

    /** Собирает текстовое представление образований для экспорта и PDF. */
    private fun composeEducation(state: ResumeFormState): String =
        state.educations
            .filter { !it.isBlankEntry() }
            .joinToString("; ") { it.describe() }

    /**
     * Best-effort разбор строки образования старого формата
     * «вуз, специальность, уровень», сохранённой до появления
     * структурированных полей. Возвращает (уровень, специальность, вуз).
     */
    private fun parseLegacyEducation(education: String): Triple<EducationLevel?, String, String>? {
        if (education.isBlank()) return null
        val parts = education.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val level = parts.mapNotNull { EducationLevel.fromLabel(it) }.firstOrNull()
        val levelLabel = level?.let { appContext.localizedString(it.labelRes) }
        val rest = parts.filter { it != levelLabel }
        return Triple(level, rest.getOrNull(1).orEmpty(), rest.getOrNull(0).orEmpty())
    }

    /**
     * Единственная точка сборки доменной модели из состояния формы —
     * используется и валидацией, и сохранением (раньше код дублировался).
     */
    private fun buildResume(state: ResumeFormState): Resume = Resume(
        id = state.resumeId,
        personalInfo = PersonalInfo(
            fullName = state.fullName,
            specialization = state.specialization,
            totalExperience = state.totalExperience,
            specializationExperience = state.specializationExperience,
            education = composeEducation(state),
            educations = state.educations.filter { !it.isBlankEntry() },
            aboutMe = state.aboutMe,
            location = state.location,
            languages = state.languages,
            email = state.email,
            phone = state.phone,
            salaryMin = state.salaryMin,
            salaryMax = state.salaryMax,
            readyToRelocate = state.readyToRelocate,
            relocationCities = state.relocationCities,
            employment = state.employment,
            workSchedule = state.workSchedule,
            photoUri = state.photoUri,
            socialLinks = state.socialLinks
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
                responsibilities = parseResponsibilityLines(projectForm.responsibilitiesText),
                teamSize = projectForm.teamSize,
                link = projectForm.link.trim()
            )
        }
    )

    /** Пересчитывает валидность и прогресс заполнения формы. */
    private fun revalidate() {
        _state.update { state ->
            val isValid = buildResume(state).isValid()
            val progress = FormValidators.progressPercent(state)
            if (state.isValid == isValid && state.progressPercent == progress) state
            else state.copy(isValid = isValid, progressPercent = progress)
        }
    }

    /** Тихо сохраняет черновик, не переводя форму в состояние «успех». */
    private fun autosaveDraft() {
        val snapshot = _state.value
        viewModelScope.launch {
            _state.update { it.copy(autosaveStatus = AutosaveStatus.SAVING) }
            try {
                repository.saveResume(buildResume(snapshot).copy(id = snapshot.resumeId))
                _state.update { it.copy(isDirty = false, autosaveStatus = AutosaveStatus.SAVED) }
                LlmLogger.logUiEvent("Form", "Draft autosaved", "Resume ID: ${snapshot.resumeId}")
            } catch (e: Exception) {
                LlmLogger.logError("Autosave failed: ${e.message}", e)
                _state.update { it.copy(autosaveStatus = AutosaveStatus.IDLE) }
            }
        }
    }

    /** Собирает доменную модель и сохраняет её в репозитории. */
    private fun submitForm() {
        revalidate()
        if (!_state.value.isValid) {
            LlmLogger.logWarning("Form submission attempted but form is not valid")
            return
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.update { it.copy(isLoading = true, error = null) }
            val resume = buildResume(_state.value)

            try {
                LlmLogger.logDatabaseOperation("INSERT/UPDATE", "resume", resume.id)
                repository.saveResume(resume)
                LlmLogger.logUiEvent("Form", "Resume saved successfully", "Resume ID: ${resume.id}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        isDirty = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось сохранить резюме: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: appContext.localizedString(R.string.error_saving)
                    )
                }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Save resume", duration)
        }
    }

    /** Сбрасывает флаг успешного сохранения после навигации. */
    fun consumeSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 300L
        private const val AUTOSAVE_DEBOUNCE_MS = 1500L
    }
}

/** Сравнивает состояние только по полям формы, игнорируя служебные флаги. */
private fun Flow<ResumeFormState>.distinctUntilChangedByFormFields() =
    distinctUntilChangedBy {
        listOf(
            it.fullName, it.specialization, it.totalExperience, it.specializationExperience,
            it.educations,
            it.email, it.phone, it.aboutMe, it.location,
            it.salaryMin, it.salaryMax, it.readyToRelocate, it.relocationCities,
            it.employment, it.workSchedule, it.photoUri,
            it.socialLinks, it.languages,
            it.operatingSystems, it.programmingLanguages, it.frameworks, it.libraries,
            it.databases, it.otherTechnologies, it.certifications, it.softSkills,
            it.professionalAchievements, it.projects
        )
    }
