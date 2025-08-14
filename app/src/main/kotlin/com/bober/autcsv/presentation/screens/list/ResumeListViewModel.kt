package com.bober.autcsv.presentation.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumeListViewModel @Inject constructor(
    private val repository: ResumeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResumeListState())
    val state: StateFlow<ResumeListState> = _state.asStateFlow()

    init {
        LlmLogger.logUiEvent("List", "ViewModel initialized", null)
        loadResumes()
    }

    private fun loadResumes() {
        LlmLogger.logDatabaseOperation("SELECT ALL", "resume", null)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.update { it.copy(isLoading = true) }
            repository.getAllResumes()
                .collect { resumes ->
                    LlmLogger.logUiEvent("List", "Resumes loaded", "Count: ${resumes.size}")
                    _state.update {
                        it.copy(
                            resumes = resumes,
                            isLoading = false
                        )
                    }
                }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Load resumes", duration)
        }
    }

    fun deleteResume(resumeId: String) {
        LlmLogger.logUserAction("Delete resume", "List", resumeId)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                LlmLogger.logDatabaseOperation("DELETE", "resume", resumeId)
                repository.deleteResume(resumeId)
                LlmLogger.logUiEvent("List", "Resume deleted successfully", resumeId)
                // Список обновится автоматически через Flow
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось удалить резюме: ${e.message}", e)
                _state.update { it.copy(error = e.message ?: "Failed to delete resume") }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Delete resume", duration)
        }
    }

    fun clearError() {
        LlmLogger.logUiEvent("List", "Error cleared", null)
        _state.update { it.copy(error = null) }
    }

    fun saveImportedResume(resume: Resume, onSaved: (String) -> Unit) {
        LlmLogger.logUserAction("Save imported resume", "List", resume.id)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                repository.saveResume(resume)
                LlmLogger.logUiEvent("List", "Imported resume saved", resume.id)
                onSaved(resume.id)
            } catch (e: Exception) {
                LlmLogger.logError("Failed to save imported resume: ${e.message}", e)
                _state.update { it.copy(error = e.message ?: "Не удалось импортировать резюме") }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Save imported resume", duration)
        }
    }
}

data class ResumeListState(
    val resumes: List<Resume> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) 