package com.bober.autcsv.presentation.screens.analysis

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.repository.ResumeRepository
import com.bober.autcsv.domain.usecase.AnalyzeCvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояние блока сопоставления резюме с вакансией. */
data class VacancyMatcherUiState(
    val vacancyText: String = "",
    val isLoading: Boolean = false,
    val result: com.bober.autcsv.domain.model.VacancyMatch? = null,
    val error: String? = null,
)

/**
 * ViewModel экрана анализа: загружает резюме, подготавливает текст для LLM,
 * запускает use case анализа и обновляет [AnalysisState].
 *
 * Дополнительно ведёт блок сопоставления с вакансией ([matcher]):
 * пользователь вставляет текст вакансии и получает оценку соответствия.
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzeCvUseCase: AnalyzeCvUseCase,
    private val resumeRepository: ResumeRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Initial)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private val _matcher = MutableStateFlow(VacancyMatcherUiState())
    val matcher: StateFlow<VacancyMatcherUiState> = _matcher.asStateFlow()

    /** Последнее загруженное резюме — нужно для сопоставления с вакансией. */
    private var loadedResume: com.bober.autcsv.domain.model.Resume? = null

    /** Обновляет текст вакансии в блоке сопоставления. */
    fun onVacancyTextChanged(text: String) {
        _matcher.update { it.copy(vacancyText = text, error = null) }
    }

    /** Запускает LLM-сопоставление резюме с введённой вакансией. */
    fun matchVacancy() {
        val resume = loadedResume ?: return
        val vacancyText = _matcher.value.vacancyText.trim()
        if (vacancyText.isBlank() || _matcher.value.isLoading) return
        viewModelScope.launch {
            _matcher.update { it.copy(isLoading = true, error = null) }
            try {
                val match = resumeRepository.matchVacancy(resume, vacancyText)
                _matcher.update { it.copy(isLoading = false, result = match) }
            } catch (e: Exception) {
                LlmLogger.logError("Сопоставление с вакансией не удалось", e)
                _matcher.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                            ?: appContext.localizedString(R.string.error_vacancy_match)
                    )
                }
            }
        }
    }

    /** Загружает резюме по ID и запускает анализ. */
    fun loadResumeAndAnalyze(resumeId: String) {
        LlmLogger.logAnalysisScreenEvent("Начинаем процесс анализа", resumeId)

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                LlmLogger.logAnalysisState("Loading")
                _state.value = AnalysisState.Loading

                // Загружаем резюме по ID
                LlmLogger.logDatabaseOperation("SELECT", "resume", resumeId)
                when (val result = resumeRepository.getResume(resumeId)) {
                    is com.bober.autcsv.data.repository.Result.Success -> {
                        val resume = result.data
                        if (resume != null) {
                            loadedResume = resume
                            LlmLogger.logAnalysisScreenEvent("Resume loaded successfully", resumeId)
                            LlmLogger.logAnalysisResult(
                                "Resume data",
                                "Name: ${resume.personalInfo.fullName}, Specialization: ${resume.personalInfo.specialization}"
                            )

                            // Конвертируем резюме в текстовое представление для анализа
                            val cvContent = convertResumeToText(resume)
                            LlmLogger.logAnalysisResult(
                                "Converted content length",
                                "${cvContent.length} characters"
                            )
                            analyzeCv(cvContent)
                        } else {
                            LlmLogger.logError("Resume not found", null)
                            _state.value =
                                AnalysisState.Error(appContext.localizedString(R.string.error_resume_not_found))
                        }
                    }

                    is com.bober.autcsv.data.repository.Result.Error -> {
                        LlmLogger.logError(
                            "Не удалось загрузить резюме: ${result.exception.message}",
                            result.exception
                        )
                        _state.value = AnalysisState.Error(
                            result.exception.message
                                ?: appContext.localizedString(R.string.error_loading)
                        )
                    }
                }
            } catch (e: Exception) {
                LlmLogger.logError("Неожиданная ошибка во время процесса анализа", e)
                _state.value = AnalysisState.Error(
                    appContext.localizedString(
                        R.string.error_unexpected_fmt,
                        e.message ?: ""
                    )
                )
            } finally {
                val duration = System.currentTimeMillis() - startTime
                LlmLogger.logPerformance("Analysis process", duration)
            }
        }
    }

    /** Повторяет анализ для указанного резюме. */
    fun retryAnalysis(resumeId: String) {
        LlmLogger.logAnalysisScreenEvent("Retrying analysis", resumeId)
        loadResumeAndAnalyze(resumeId)
    }

    /** Вызывает use case анализа и обновляет состояние на основе результата. */
    private fun analyzeCv(cvContent: String) {
        LlmLogger.logAnalysisStart(cvContent.length)

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                LlmLogger.logAnalysisState("Анализируем")
                LlmLogger.logApiRequest("CV Analysis", cvContent.length)

                analyzeCvUseCase(cvContent)
                    .onSuccess { analysis ->
                        val duration = System.currentTimeMillis() - startTime
                        LlmLogger.logPerformance("CV Analysis", duration)
                        LlmLogger.logAnalysisState("Success")
                        LlmLogger.logAnalysisResult(
                            "Analysis completed",
                            "Completeness: ${analysis.completenessAnalysis.length} chars, Logic: ${analysis.logicAnalysis.length} chars, Recommendations: ${analysis.recommendations.size}"
                        )
                        _state.value = AnalysisState.Success(analysis)
                    }
                    .onFailure { error ->
                        val duration = System.currentTimeMillis() - startTime
                        LlmLogger.logPerformance("CV Analysis (failed)", duration)
                        LlmLogger.logError("Анализ завершился с ошибкой", error)
                        _state.value = AnalysisState.Error(
                            error.message ?: appContext.localizedString(R.string.error_unknown)
                        )
                    }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                LlmLogger.logPerformance("CV Analysis (exception)", duration)
                LlmLogger.logError("Неожиданная ошибка во время анализа", e)
                _state.value = AnalysisState.Error(
                    appContext.localizedString(
                        R.string.error_unexpected_fmt,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    /** Преобразует резюме в текстовую форму, удобную для анализа LLM. */
    private fun convertResumeToText(resume: com.bober.autcsv.domain.model.Resume): String {
        LlmLogger.logAnalysisScreenEvent("Converting resume to text", resume.id)

        return buildString {
            appendLine("ПЕРСОНАЛЬНАЯ ИНФОРМАЦИЯ")
            appendLine("Имя: ${resume.personalInfo.fullName}")
            appendLine("Специализация: ${resume.personalInfo.specialization}")
            appendLine("Общий опыт: ${resume.personalInfo.totalExperience}")
            appendLine("Опыт по специализации: ${resume.personalInfo.specializationExperience}")
            appendLine("Email: ${resume.personalInfo.email}")
            appendLine("Телефон: ${resume.personalInfo.phone}")
            appendLine("Местоположение: ${resume.personalInfo.location}")
            appendLine("Образование: ${resume.personalInfo.education}")
            appendLine("О себе: ${resume.personalInfo.aboutMe}")
            appendLine()

            appendLine("ЯЗЫКИ")
            resume.personalInfo.languages.forEach { language ->
                appendLine("• ${language.name}: ${language.level}")
            }
            appendLine()

            appendLine("ПРОФЕССИОНАЛЬНЫЕ НАВЫКИ")
            appendLine(
                "Операционные системы: ${
                    resume.professionalSkills.operatingSystems.joinToString(
                        ", "
                    )
                }"
            )
            appendLine(
                "Языки программирования: ${
                    resume.professionalSkills.programmingLanguages.joinToString(
                        ", "
                    )
                }"
            )
            appendLine("Фреймворки: ${resume.professionalSkills.frameworks.joinToString(", ")}")
            appendLine("Библиотеки: ${resume.professionalSkills.libraries.joinToString(", ")}")
            appendLine("Базы данных: ${resume.professionalSkills.databases.joinToString(", ")}")
            appendLine(
                "Другие технологии: ${
                    resume.professionalSkills.otherTechnologies.joinToString(
                        ", "
                    )
                }"
            )
            appendLine(
                "Достижения: ${
                    resume.professionalSkills.professionalAchievements.joinToString(
                        ", "
                    )
                }"
            )
            appendLine("Сертификации: ${resume.professionalSkills.certifications.joinToString(", ")}")
            appendLine()

            appendLine("ПРОЕКТЫ")
            resume.projects.forEach { project ->
                appendLine("Название: ${project.name}")
                appendLine("Роль: ${project.role}")
                appendLine("Длительность: ${project.duration}")
                appendLine("Описание: ${project.description}")
                appendLine("Технологии: ${project.technologies.joinToString(", ")}")
                appendLine("Обязанности: ${project.responsibilities.joinToString(", ")}")
                appendLine("Размер команды: ${project.teamSize}")
                appendLine()
            }

            appendLine("СВОДКА")
            appendLine(resume.summary)
        }.also {
            LlmLogger.logAnalysisResult(
                "Converted text",
                "Total length: ${it.length}, Projects: ${resume.projects.size}, Languages: ${resume.personalInfo.languages.size}"
            )
        }
    }
} 