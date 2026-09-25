package com.bober.autcsv.presentation.screens.coverletter

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.model.CoverLetter
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Состояние редактора письма. */
data class CoverLetterEditorState(
    val vacancyText: String = "",
    val content: String = "",
    /** id редактируемого письма; null — черновик новой генерации. */
    val editingId: String? = null,
    val isGenerating: Boolean = false,
    /** Одноразовое сообщение для snackbar (ошибки/подтверждения). */
    val message: String? = null,
)

/**
 * ViewModel экрана сопроводительного письма: генерация LLM по вакансии,
 * редактирование, сохранение и удаление писем текущего резюме.
 */
@HiltViewModel
class CoverLetterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ResumeRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val resumeId: String = checkNotNull(savedStateHandle["resumeId"])

    private val _editor = MutableStateFlow(CoverLetterEditorState())
    val editor: StateFlow<CoverLetterEditorState> = _editor.asStateFlow()

    val letters: StateFlow<List<CoverLetter>> = repository.coverLettersFor(resumeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var resume: Resume? = null

    init {
        viewModelScope.launch {
            when (val result = repository.getResume(resumeId)) {
                is com.bober.autcsv.data.repository.Result.Success -> resume = result.data
                else -> Unit
            }
        }
    }

    fun onVacancyChange(value: String) =
        _editor.update { it.copy(vacancyText = value) }

    fun onContentChange(value: String) =
        _editor.update { it.copy(content = value) }

    fun consumeMessage() = _editor.update { it.copy(message = null) }

    fun startNewDraft() = _editor.update {
        it.copy(content = "", editingId = null)
    }

    fun edit(letter: CoverLetter) = _editor.update {
        it.copy(
            vacancyText = letter.vacancyText,
            content = letter.content,
            editingId = letter.id,
        )
    }

    /** Генерирует письмо по вакансии; результат попадает в редактор без сохранения. */
    fun generate() {
        val state = _editor.value
        val targetResume = resume ?: return
        if (state.isGenerating) return
        if (state.vacancyText.isBlank()) {
            _editor.update {
                it.copy(message = appContext.localizedString(R.string.cover_letter_need_vacancy))
            }
            return
        }
        _editor.update { it.copy(isGenerating = true) }
        viewModelScope.launch {
            runCatching { repository.generateCoverLetter(targetResume, state.vacancyText.trim()) }
                .onSuccess { text ->
                    _editor.update {
                        it.copy(content = text, editingId = null, isGenerating = false)
                    }
                }
                .onFailure { error ->
                    LlmLogger.logError("Генерация сопроводительного письма не удалась", error)
                    _editor.update {
                        it.copy(
                            isGenerating = false,
                            message = appContext.localizedString(
                                R.string.cover_letter_generate_failed_fmt,
                                error.message ?: ""
                            )
                        )
                    }
                }
        }
    }

    /** Сохраняет письмо из редактора (создание или обновление). */
    fun save() {
        val state = _editor.value
        if (state.content.isBlank()) return
        val now = System.currentTimeMillis()
        val existing = state.editingId?.let { id -> letters.value.firstOrNull { it.id == id } }
        val letter = CoverLetter(
            id = existing?.id ?: UUID.randomUUID().toString(),
            resumeId = resumeId,
            vacancyTitle = vacancyTitle(state.vacancyText),
            vacancyText = state.vacancyText.trim(),
            content = state.content.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        viewModelScope.launch {
            repository.saveCoverLetter(letter)
            _editor.update {
                it.copy(
                    editingId = letter.id,
                    message = appContext.localizedString(R.string.cover_letter_saved)
                )
            }
        }
    }

    fun delete(letterId: String) {
        viewModelScope.launch {
            repository.deleteCoverLetter(letterId)
            if (_editor.value.editingId == letterId) startNewDraft()
        }
    }

    /** Подпись письма в списке — первая строка вакансии, обрезанная до 60 символов. */
    private fun vacancyTitle(vacancyText: String): String {
        val firstLine = vacancyText.trim().lines().firstOrNull { it.isNotBlank() } ?: ""
        return firstLine.take(60)
    }
}
