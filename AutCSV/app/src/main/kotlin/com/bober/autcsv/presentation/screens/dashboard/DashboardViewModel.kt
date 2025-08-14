package com.bober.autcsv.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ResumeRepository,
) : ViewModel() {
    private val _filters = MutableStateFlow(DashboardFilters())
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(_resumes, _filters, _error) { resumes, filters, error ->
        when {
            error != null -> DashboardUiState.Error(error)
            else -> {
                val filteredResumes = resumes.filter { resume ->
                    (filters.experienceLevels.isEmpty() || matchesAnyExperience(resume, filters.experienceLevels)) &&
                    (filters.technologies.isEmpty() || matchesAnyTechnology(resume, filters.technologies)) &&
                    (filters.languages.isEmpty() || matchesAnyLanguage(resume, filters.languages)) &&
                    (filters.specializations.isEmpty() || matchesAnySpecialization(resume, filters.specializations))
                }
                
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

    fun updateFilters(filters: DashboardFilters) {
        _filters.value = filters
    }

    fun clearFilters() {
        _filters.value = DashboardFilters()
    }

    private fun parseYearsFromText(text: String): Int {
        val match = Regex("(\\d+)").find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun loadResumes() {
        viewModelScope.launch {
            try {
                repository.getAllResumes()
                    .catch { e ->
                        _error.value = e.message ?: "Unknown error"
                    }
                    .collect { resumes ->
                        _resumes.value = resumes
                        _error.value = null
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
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
        return resume.professionalSkills.getAllTechnologies().any {
            it.contains(tech, ignoreCase = true)
        } || resume.projects.any { project ->
            project.technologies.any { it.contains(tech, ignoreCase = true) }
        }
    }

    private fun matchesLanguage(resume: Resume, lang: String): Boolean {
        return resume.personalInfo.languages.any { 
            it.name.contains(lang, ignoreCase = true) 
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
        val specializations = resumes.map { it.personalInfo.specialization }.distinct().sorted()
        val technologies = resumes.flatMap { resume ->
            resume.professionalSkills.getAllTechnologies() + resume.projects.flatMap { it.technologies }
        }.distinct().sorted()
        val languages = resumes.flatMap { it.personalInfo.languages }.map { it.name }.distinct().sorted()
        
        return AvailableFilters(
            specializations = specializations,
            programmingLanguages = technologies.filter { tech ->
                tech.contains("java", ignoreCase = true) || tech.contains("kotlin", ignoreCase = true) ||
                tech.contains("python", ignoreCase = true) || tech.contains("javascript", ignoreCase = true) ||
                tech.contains("typescript", ignoreCase = true) || tech.contains("c++", ignoreCase = true) ||
                tech.contains("c#", ignoreCase = true) || tech.contains("go", ignoreCase = true) ||
                tech.contains("rust", ignoreCase = true) || tech.contains("swift", ignoreCase = true)
            },
            frameworks = technologies.filter { tech ->
                tech.contains("spring", ignoreCase = true) || tech.contains("react", ignoreCase = true) ||
                tech.contains("angular", ignoreCase = true) || tech.contains("vue", ignoreCase = true) ||
                tech.contains("django", ignoreCase = true) || tech.contains("flask", ignoreCase = true) ||
                tech.contains("express", ignoreCase = true) || tech.contains("laravel", ignoreCase = true) ||
                tech.contains("asp.net", ignoreCase = true) || tech.contains("rails", ignoreCase = true)
            },
            libraries = technologies.filter { tech ->
                tech.contains("junit", ignoreCase = true) || tech.contains("mockito", ignoreCase = true) ||
                tech.contains("axios", ignoreCase = true) || tech.contains("lodash", ignoreCase = true) ||
                tech.contains("pandas", ignoreCase = true) || tech.contains("numpy", ignoreCase = true) ||
                tech.contains("jquery", ignoreCase = true) || tech.contains("bootstrap", ignoreCase = true)
            },
            databases = technologies.filter { tech ->
                tech.contains("mysql", ignoreCase = true) || tech.contains("postgresql", ignoreCase = true) ||
                tech.contains("mongodb", ignoreCase = true) || tech.contains("redis", ignoreCase = true) ||
                tech.contains("sqlite", ignoreCase = true) || tech.contains("oracle", ignoreCase = true) ||
                tech.contains("sql server", ignoreCase = true) || tech.contains("elasticsearch", ignoreCase = true)
            },
            otherTechnologies = technologies.filter { tech ->
                !tech.contains("java", ignoreCase = true) && !tech.contains("kotlin", ignoreCase = true) &&
                !tech.contains("python", ignoreCase = true) && !tech.contains("javascript", ignoreCase = true) &&
                !tech.contains("typescript", ignoreCase = true) && !tech.contains("c++", ignoreCase = true) &&
                !tech.contains("c#", ignoreCase = true) && !tech.contains("go", ignoreCase = true) &&
                !tech.contains("rust", ignoreCase = true) && !tech.contains("swift", ignoreCase = true) &&
                !tech.contains("spring", ignoreCase = true) && !tech.contains("react", ignoreCase = true) &&
                !tech.contains("angular", ignoreCase = true) && !tech.contains("vue", ignoreCase = true) &&
                !tech.contains("django", ignoreCase = true) && !tech.contains("flask", ignoreCase = true) &&
                !tech.contains("express", ignoreCase = true) && !tech.contains("laravel", ignoreCase = true) &&
                !tech.contains("asp.net", ignoreCase = true) && !tech.contains("rails", ignoreCase = true) &&
                !tech.contains("junit", ignoreCase = true) && !tech.contains("mockito", ignoreCase = true) &&
                !tech.contains("axios", ignoreCase = true) && !tech.contains("lodash", ignoreCase = true) &&
                !tech.contains("pandas", ignoreCase = true) && !tech.contains("numpy", ignoreCase = true) &&
                !tech.contains("jquery", ignoreCase = true) && !tech.contains("bootstrap", ignoreCase = true) &&
                !tech.contains("mysql", ignoreCase = true) && !tech.contains("postgresql", ignoreCase = true) &&
                !tech.contains("mongodb", ignoreCase = true) && !tech.contains("redis", ignoreCase = true) &&
                !tech.contains("sqlite", ignoreCase = true) && !tech.contains("oracle", ignoreCase = true) &&
                !tech.contains("sql server", ignoreCase = true) && !tech.contains("elasticsearch", ignoreCase = true)
            },
            languages = languages
        )
    }
}

data class DashboardFilters(
    val experienceLevels: Set<String> = emptySet(),
    val technologies: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    val specializations: Set<String> = emptySet(),
) {
    fun hasActiveFilters(): Boolean = 
        experienceLevels.isNotEmpty() || technologies.isNotEmpty() || languages.isNotEmpty() || specializations.isNotEmpty()
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