package com.bober.autcsv.presentation.screens.dashboard

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel дашборда: подгружает список резюме, применяет фильтры,
 * формирует состояние UI и обрабатывает ошибки.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ResumeRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _filters = MutableStateFlow(DashboardFilters())
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(_resumes, _filters, _error) { resumes, filters, error ->
        when {
            error != null -> DashboardUiState.Error(error)
            else -> {
                val filteredResumes = applyFilters(resumes, filters)

                val availableFilters = extractAvailableFilters(resumes)

                DashboardUiState.Success(
                    resumes = filteredResumes,
                    filters = filters,
                    availableFilters = availableFilters
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    init {
        loadResumes()
    }

    /** Обновляет текущий набор фильтров. */
    fun updateFilters(filters: DashboardFilters) {
        _filters.value = filters
    }

    /** Сбрасывает фильтры к значениям по умолчанию. */
    fun clearFilters() {
        _filters.value = DashboardFilters()
    }

    /** Переключает избранное у резюме. */
    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    /** Переводит резюме на следующий статус пайплайна (по кругу). */
    fun cycleStatus(id: String) {
        viewModelScope.launch {
            val current = _resumes.value.firstOrNull { it.id == id } ?: return@launch
            val order = com.bober.autcsv.domain.model.CandidateStatus.entries
            val next = order[(order.indexOf(current.status) + 1).mod(order.size)]
            repository.setStatus(id, next)
        }
    }

    /** Дублирует резюме. */
    fun duplicateResume(id: String) {
        viewModelScope.launch { repository.duplicateResume(id) }
    }

    /** Применяет фильтры и сортировку к списку резюме. */
    private fun applyFilters(resumes: List<Resume>, filters: DashboardFilters): List<Resume> {
        val filtered = resumes.filter { resume ->
            (filters.experienceLevels.isEmpty() || matchesAnyExperience(
                resume,
                filters.experienceLevels
            )) &&
                    (filters.technologies.isEmpty() || matchesAnyTechnology(
                        resume,
                        filters.technologies
                    )) &&
                    (filters.languages.isEmpty() || matchesAnyLanguage(
                        resume,
                        filters.languages
                    )) &&
                    (filters.specializations.isEmpty() || matchesAnySpecialization(
                        resume,
                        filters.specializations
                    )) &&
                    (!filters.favoritesOnly || resume.isFavorite) &&
                    (filters.status == null || resume.status == filters.status) &&
                    (!filters.readyToRelocateOnly ||
                            // Значение — свободный текст, выбранный при текущей локали
                            resume.personalInfo.readyToRelocate.startsWith("Готов") ||
                            resume.personalInfo.readyToRelocate.startsWith("Ready")) &&
                    (filters.minSalary == null ||
                            (resume.personalInfo.salaryMin.toIntOrNull()
                                ?: resume.personalInfo.salaryMax.toIntOrNull()
                                ?: 0) >= filters.minSalary)
        }
        val sorted = when (filters.sortBy) {
            SortOrder.LAST_MODIFIED -> filtered.sortedBy { it.lastModified }
            SortOrder.SALARY -> filtered.sortedBy {
                it.personalInfo.salaryMin.toIntOrNull() ?: 0
            }

            SortOrder.EXPERIENCE -> filtered.sortedBy {
                parseYearsFromText(it.personalInfo.totalExperience)
            }

            SortOrder.NAME -> filtered.sortedBy { it.personalInfo.fullName.lowercase() }
        }
        // Направление: базовая сортировка по возрастанию, переключатель её разворачивает
        return if (filters.sortAscending) sorted else sorted.reversed()
    }

    /** Загружает резюме из репозитория и собирает доступные фильтры. */
    private fun loadResumes() {
        viewModelScope.launch {
            try {
                repository.getAllResumes()
                    .catch { e ->
                        _error.value =
                            e.message ?: appContext.localizedString(R.string.error_unknown)
                    }
                    .collect { resumes ->
                        _resumes.value = resumes
                        _error.value = null
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: appContext.localizedString(R.string.error_unknown)
            }
        }
    }

    private fun parseYearsFromText(text: String): Int {
        val match = Regex("(\\d+)").find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun matchesExperience(resume: Resume, level: String): Boolean {
        val years = parseYearsFromText(resume.personalInfo.specializationExperience)
        return when (level) {
            "Junior" -> years < 3
            "Middle" -> years in 3..5
            "Senior" -> years > 5
            else -> true
        }
    }

    private fun matchesTechnology(resume: Resume, tech: String): Boolean {
        val target = normalizeLabel(tech)
        return resume.professionalSkills.getAllTechnologies()
            .map { normalizeLabel(it) }
            .any { it.contains(target, ignoreCase = true) } ||
                resume.projects.any { project ->
                    project.technologies
                        .map { normalizeLabel(it) }
                        .any { it.contains(target, ignoreCase = true) }
                }
    }

    private fun matchesLanguage(resume: Resume, lang: String): Boolean {
        val target = normalizeLabel(lang)
        return resume.personalInfo.languages.any {
            normalizeLabel(it.name).contains(target, ignoreCase = true)
        }
    }

    private fun matchesSpecialization(resume: Resume, spec: String): Boolean {
        return resume.personalInfo.specialization.contains(spec, ignoreCase = true)
    }

    private fun matchesAnyExperience(resume: Resume, levels: Set<String>): Boolean {
        return levels.any { level -> matchesExperience(resume, level) }
    }

    private fun matchesAnyTechnology(resume: Resume, technologies: Set<String>): Boolean {
        return technologies.any { tech -> matchesTechnology(resume, tech) }
    }

    private fun matchesAnyLanguage(resume: Resume, languages: Set<String>): Boolean {
        return languages.any { lang -> matchesLanguage(resume, lang) }
    }

    private fun matchesAnySpecialization(resume: Resume, specializations: Set<String>): Boolean {
        return specializations.any { spec -> matchesSpecialization(resume, spec) }
    }

    private fun extractAvailableFilters(resumes: List<Resume>): AvailableFilters {
        val specializations = resumes
            .map { normalizeLabel(it.personalInfo.specialization) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

        val rawTechnologies = resumes.flatMap { resume ->
            resume.professionalSkills.getAllTechnologies() +
                    resume.projects.flatMap { it.technologies }
        }
        val technologies = rawTechnologies
            .flatMap { splitIfMultiTech(normalizeLabel(it)) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

        val languages = resumes
            .flatMap { it.personalInfo.languages }
            .map { normalizeLabel(it.name) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

        return AvailableFilters(
            specializations = specializations,
            programmingLanguages = technologies.filter { tech ->
                tech.contains("java", ignoreCase = true) || tech.contains(
                    "kotlin",
                    ignoreCase = true
                ) ||
                        tech.contains("python", ignoreCase = true) || tech.contains(
                    "javascript",
                    ignoreCase = true
                ) ||
                        tech.contains("typescript", ignoreCase = true) || tech.contains(
                    "c++",
                    ignoreCase = true
                ) ||
                        tech.contains("c#", ignoreCase = true) || tech.contains(
                    "go",
                    ignoreCase = true
                ) ||
                        tech.contains("rust", ignoreCase = true) || tech.contains(
                    "swift",
                    ignoreCase = true
                )
            },
            frameworks = technologies.filter { tech ->
                tech.contains("spring", ignoreCase = true) || tech.contains(
                    "react",
                    ignoreCase = true
                ) ||
                        tech.contains("angular", ignoreCase = true) || tech.contains(
                    "vue",
                    ignoreCase = true
                ) ||
                        tech.contains("django", ignoreCase = true) || tech.contains(
                    "flask",
                    ignoreCase = true
                ) ||
                        tech.contains("express", ignoreCase = true) || tech.contains(
                    "laravel",
                    ignoreCase = true
                ) ||
                        tech.contains("asp.net", ignoreCase = true) || tech.contains(
                    "rails",
                    ignoreCase = true
                )
            },
            libraries = technologies.filter { tech ->
                tech.contains("junit", ignoreCase = true) || tech.contains(
                    "mockito",
                    ignoreCase = true
                ) ||
                        tech.contains("axios", ignoreCase = true) || tech.contains(
                    "lodash",
                    ignoreCase = true
                ) ||
                        tech.contains("pandas", ignoreCase = true) || tech.contains(
                    "numpy",
                    ignoreCase = true
                ) ||
                        tech.contains("jquery", ignoreCase = true) || tech.contains(
                    "bootstrap",
                    ignoreCase = true
                )
            },
            databases = technologies.filter { tech ->
                tech.contains("mysql", ignoreCase = true) || tech.contains(
                    "postgresql",
                    ignoreCase = true
                ) ||
                        tech.contains("mongodb", ignoreCase = true) || tech.contains(
                    "redis",
                    ignoreCase = true
                ) ||
                        tech.contains("sqlite", ignoreCase = true) || tech.contains(
                    "oracle",
                    ignoreCase = true
                ) ||
                        tech.contains(
                            "sql server",
                            ignoreCase = true
                        ) || tech.contains("elasticsearch", ignoreCase = true)
            },
            otherTechnologies = technologies.filter { tech ->
                !tech.contains("java", ignoreCase = true) && !tech.contains(
                    "kotlin",
                    ignoreCase = true
                ) &&
                        !tech.contains("python", ignoreCase = true) && !tech.contains(
                    "javascript",
                    ignoreCase = true
                ) &&
                        !tech.contains("typescript", ignoreCase = true) && !tech.contains(
                    "c++",
                    ignoreCase = true
                ) &&
                        !tech.contains("c#", ignoreCase = true) && !tech.contains(
                    "go",
                    ignoreCase = true
                ) &&
                        !tech.contains("rust", ignoreCase = true) && !tech.contains(
                    "swift",
                    ignoreCase = true
                ) &&
                        !tech.contains("spring", ignoreCase = true) && !tech.contains(
                    "react",
                    ignoreCase = true
                ) &&
                        !tech.contains("angular", ignoreCase = true) && !tech.contains(
                    "vue",
                    ignoreCase = true
                ) &&
                        !tech.contains("django", ignoreCase = true) && !tech.contains(
                    "flask",
                    ignoreCase = true
                ) &&
                        !tech.contains("express", ignoreCase = true) && !tech.contains(
                    "laravel",
                    ignoreCase = true
                ) &&
                        !tech.contains("asp.net", ignoreCase = true) && !tech.contains(
                    "rails",
                    ignoreCase = true
                ) &&
                        !tech.contains("junit", ignoreCase = true) && !tech.contains(
                    "mockito",
                    ignoreCase = true
                ) &&
                        !tech.contains("axios", ignoreCase = true) && !tech.contains(
                    "lodash",
                    ignoreCase = true
                ) &&
                        !tech.contains("pandas", ignoreCase = true) && !tech.contains(
                    "numpy",
                    ignoreCase = true
                ) &&
                        !tech.contains("jquery", ignoreCase = true) && !tech.contains(
                    "bootstrap",
                    ignoreCase = true
                ) &&
                        !tech.contains("mysql", ignoreCase = true) && !tech.contains(
                    "postgresql",
                    ignoreCase = true
                ) &&
                        !tech.contains("mongodb", ignoreCase = true) && !tech.contains(
                    "redis",
                    ignoreCase = true
                ) &&
                        !tech.contains("sqlite", ignoreCase = true) && !tech.contains(
                    "oracle",
                    ignoreCase = true
                ) &&
                        !tech.contains(
                            "sql server",
                            ignoreCase = true
                        ) && !tech.contains("elasticsearch", ignoreCase = true)
            },
            languages = languages
        )
    }

    private fun normalizeLabel(input: String): String =
        input.trim().replace(Regex("\\s+"), " ")

    private fun splitIfMultiTech(token: String): List<String> {
        if (token.isBlank()) return emptyList()
        // Do not split if token already uses common separators
        if (token.contains(',') || token.contains('•') || token.contains('·') || token.contains(';')) return listOf(
            token
        )
        val parts = token.split(Regex("\\s+"))
        // Heuristic: split only if it looks like concatenated standalone techs (>=3 capitalized words)
        return if (parts.size >= 3 && parts.all {
                it.firstOrNull()?.isUpperCase() == true
            }) parts else listOf(token)
    }
}

/** Ключ сортировки списка резюме; направление задаётся отдельно. */
enum class SortOrder(@StringRes val labelRes: Int) {
    LAST_MODIFIED(R.string.sort_by_modified),
    SALARY(R.string.sort_by_salary),
    EXPERIENCE(R.string.sort_by_experience),
    NAME(R.string.sort_by_name);
}

data class DashboardFilters(
    val experienceLevels: Set<String> = emptySet(),
    val technologies: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    val specializations: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
    val readyToRelocateOnly: Boolean = false,
    val minSalary: Int? = null,
    val status: com.bober.autcsv.domain.model.CandidateStatus? = null,
    val sortBy: SortOrder = SortOrder.LAST_MODIFIED,
    val sortAscending: Boolean = false,
) {
    fun hasActiveFilters(): Boolean =
        experienceLevels.isNotEmpty() || technologies.isNotEmpty() ||
                languages.isNotEmpty() || specializations.isNotEmpty() ||
                favoritesOnly || readyToRelocateOnly || minSalary != null || status != null
}

data class AvailableFilters(
    val specializations: List<String> = emptyList(),
    val programmingLanguages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val otherTechnologies: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
)

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val resumes: List<Resume>,
        val filters: DashboardFilters,
        val availableFilters: AvailableFilters,
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
} 