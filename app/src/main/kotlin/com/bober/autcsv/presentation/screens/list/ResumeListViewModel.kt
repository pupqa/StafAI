package com.bober.autcsv.presentation.screens.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumeListViewModel @Inject constructor(
    private val repository: ResumeRepository,
    private val pdfStyleStore: com.bober.autcsv.core.pdf.PdfStyleStore,
    @ApplicationContext private val appContext: Context,
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
                _state.update {
                    it.copy(
                        error = e.message ?: appContext.localizedString(R.string.error_deleting)
                    )
                }
            }
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("Delete resume", duration)
        }
    }

    fun clearError() {
        LlmLogger.logUiEvent("List", "Error cleared", null)
        _state.update { it.copy(error = null) }
    }

    /** Откат мягкого удаления для снекбара «Отменить». */
    fun restoreResume(resumeId: String) {
        LlmLogger.logUserAction("Undo delete resume", "List", resumeId)
        viewModelScope.launch {
            try {
                repository.restoreResume(resumeId)
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось восстановить резюме: ${e.message}", e)
                _state.update {
                    it.copy(
                        error = e.message ?: appContext.localizedString(R.string.error_unknown)
                    )
                }
            }
        }
    }

    fun toggleFavorite(resumeId: String) {
        LlmLogger.logUserAction("Toggle favorite", "List", resumeId)
        viewModelScope.launch {
            try {
                repository.toggleFavorite(resumeId)
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось изменить избранное: ${e.message}", e)
            }
        }
    }

    /** Создаёт копию резюме и возвращает её id (для перехода/снекбара). */
    fun duplicateResume(resumeId: String, onCreated: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                onCreated(repository.duplicateResume(resumeId)?.id)
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось дублировать резюме: ${e.message}", e)
                onCreated(null)
            }
        }
    }

    /**
     * Генерирует PDF актуальным стилем из [PdfStyleStore] и открывает системный
     * диалог «Поделиться». Activity-контекст не нужен: NEW_TASK флаг достаточен.
     */
    fun sharePdf(resumeId: String) {
        viewModelScope.launch {
            try {
                val resume = when (val result = repository.getResume(resumeId)) {
                    is com.bober.autcsv.data.repository.Result.Success -> result.data
                    else -> null
                } ?: return@launch
                val path = repository.generatePdf(
                    resume,
                    pdfStyleStore.templateType,
                    pdfStyleStore.accentColor,
                    pdfStyleStore.sectionOrder,
                )
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.provider",
                    java.io.File(path),
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(
                    android.content.Intent.createChooser(
                        intent,
                        appContext.getString(R.string.share_resume_chooser)
                    )
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось поделиться PDF: ${e.message}", e)
                _state.update {
                    it.copy(
                        error = e.message ?: appContext.localizedString(
                            R.string.error_share_pdf_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
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
                _state.update {
                    it.copy(
                        error = e.message
                            ?: appContext.localizedString(R.string.error_import_failed)
                    )
                }
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