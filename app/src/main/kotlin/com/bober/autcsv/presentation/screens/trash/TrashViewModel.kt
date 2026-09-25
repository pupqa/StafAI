package com.bober.autcsv.presentation.screens.trash

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

data class TrashState(
    val resumes: List<Resume> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Идентификатор последнего восстановленного резюме — для снекбара. */
    val lastRestoredName: String? = null,
    /** Подтверждение очистки корзины. */
    val showEmptyTrashDialog: Boolean = false,
    /** Резюме, ожидаемое окончательного удаления по кнопке «Удалить навсегда». */
    val pendingPermanentDeleteId: String? = null,
)

@HiltViewModel
/**
 * ViewModel корзины: подписка на поток удалённых резюме, восстановление,
 * окончательное удаление и очистка корзины.
 */
class TrashViewModel @Inject constructor(
    private val repository: ResumeRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TrashState())
    val state: StateFlow<TrashState> = _state.asStateFlow()

    init {
        LlmLogger.logUiEvent("Trash", "ViewModel initialized", null)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.getDeletedResumes().collect { resumes ->
                    LlmLogger.logUiEvent(
                        "Trash",
                        "Deleted resumes loaded",
                        "Count: ${resumes.size}"
                    )
                    _state.update {
                        it.copy(resumes = resumes, isLoading = false, error = null)
                    }
                }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось загрузить корзину: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: appContext.localizedString(R.string.trash_load_error)
                    )
                }
            }
        }
    }

    /** Восстанавливает резюме из корзины. */
    fun restoreResume(resume: Resume) {
        LlmLogger.logUserAction("Restore resume", "Trash", resume.id)
        viewModelScope.launch {
            try {
                repository.restoreResume(resume.id)
                _state.update {
                    it.copy(lastRestoredName = resume.personalInfo.fullName.ifBlank { resume.id })
                }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось восстановить резюме: ${e.message}", e)
                _state.update {
                    it.copy(
                        error = e.message
                            ?: appContext.localizedString(R.string.trash_restore_error)
                    )
                }
            }
        }
    }

    /** Окончательно удаляет одно резюме из корзины. */
    fun deletePermanently(id: String) {
        LlmLogger.logUserAction("Delete resume permanently", "Trash", id)
        viewModelScope.launch {
            try {
                repository.deleteResumePermanently(id)
                _state.update { it.copy(pendingPermanentDeleteId = null) }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось удалить резюме: ${e.message}", e)
                _state.update {
                    it.copy(
                        error = e.message ?: appContext.localizedString(R.string.trash_delete_error)
                    )
                }
            }
        }
    }

    /** Окончательно очищает корзину. */
    fun emptyTrash() {
        LlmLogger.logUserAction("Empty trash", "Trash", null)
        viewModelScope.launch {
            try {
                repository.emptyTrash()
                _state.update { it.copy(showEmptyTrashDialog = false) }
            } catch (e: Exception) {
                LlmLogger.logError("Не удалось очистить корзину: ${e.message}", e)
                _state.update {
                    it.copy(
                        showEmptyTrashDialog = false,
                        error = e.message ?: appContext.localizedString(R.string.trash_empty_error)
                    )
                }
            }
        }
    }

    fun requestEmptyTrash() {
        _state.update { it.copy(showEmptyTrashDialog = true) }
    }

    fun dismissEmptyTrashDialog() {
        _state.update { it.copy(showEmptyTrashDialog = false) }
    }

    fun requestPermanentDelete(id: String) {
        _state.update { it.copy(pendingPermanentDeleteId = id) }
    }

    fun dismissPermanentDelete() {
        _state.update { it.copy(pendingPermanentDeleteId = null) }
    }

    fun consumeRestoredMessage() {
        _state.update { it.copy(lastRestoredName = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
