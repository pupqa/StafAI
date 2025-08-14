package com.bober.autcsv.presentation.screens.preview

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ResumePreviewState(
    val resume: Resume? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPdfGenerating: Boolean = false,
    val isPdfReady: Boolean = false,
    val pdfGenerationError: String? = null,
    val selectedTemplate: PdfTemplateType = PdfTemplateType.getDefault(),
    val isAnalyzing: Boolean = false,
    val analyzeError: String? = null,
    val aiSuggestionsApplied: Boolean = false,
)

@HiltViewModel
class ResumePreviewViewModel @Inject constructor(
    private val repository: ResumeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var onNavigateToAnalysis: ((String) -> Unit)? = null

    fun setNavigationCallback(callback: (String) -> Unit) {
        onNavigateToAnalysis = callback
    }

    private val _state = MutableStateFlow(ResumePreviewState())
    val state: StateFlow<ResumePreviewState> = _state

    private var pdfPath: String? = null

    fun onEvent(event: ResumePreviewEvent) {
        when (event) {
            is ResumePreviewEvent.LoadResume -> loadResume(event.id)
            is ResumePreviewEvent.SelectTemplate -> {
                _state.update { it.copy(selectedTemplate = event.templateType) }
                _state.value.resume?.let { generatePdf(it) }
            }

            is ResumePreviewEvent.SharePdf -> sharePdf()
            is ResumePreviewEvent.DownloadPdf -> downloadPdf()
            is ResumePreviewEvent.AnalyzeResume -> analyzeResume()
            is ResumePreviewEvent.ApplyAiSuggestions -> applyAiSuggestions()
            is ResumePreviewEvent.ExportResume -> exportResume()
        }
    }

    private fun loadResume(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = repository.getResume(id)) {
                    is com.bober.autcsv.data.repository.Result.Success -> {
                        _state.update { it.copy(resume = result.data, isLoading = false) }
                        result.data?.let { generatePdf(it) }
                    }

                    is com.bober.autcsv.data.repository.Result.Error -> {
                        _state.update {
                            it.copy(
                                error = result.exception.message ?: "Не удалось загрузить резюме",
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Не удалось загрузить резюме",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun generatePdf(resume: Resume) {
        viewModelScope.launch {
            _state.update { it.copy(isPdfGenerating = true, pdfGenerationError = null) }
            try {
                pdfPath = repository.generatePdf(resume, _state.value.selectedTemplate)
                _state.update {
                    it.copy(
                        isPdfGenerating = false,
                        isPdfReady = true,
                        pdfGenerationError = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPdfGenerating = false,
                        isPdfReady = false,
                        pdfGenerationError = "Failed to generate PDF: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun sharePdf() {
        if (_state.value.isPdfGenerating) {
            _state.update { it.copy(pdfGenerationError = "Please wait while PDF is being generated") }
            return
        }

        if (!_state.value.isPdfReady) {
            _state.value.resume?.let { generatePdf(it) }
            return
        }

        pdfPath?.let { path ->
            try {
                val file = File(path)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooserIntent = Intent.createChooser(intent, "Share Resume").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                _state.update { it.copy(pdfGenerationError = "Failed to share PDF: ${e.localizedMessage}") }
            }
        } ?: run {
            _state.update { it.copy(pdfGenerationError = "PDF file not found") }
        }
    }

    private fun downloadPdf() {
        if (_state.value.isPdfGenerating) {
            _state.update { it.copy(pdfGenerationError = "Please wait while PDF is being generated") }
            return
        }

        if (!_state.value.isPdfReady) {
            _state.value.resume?.let { generatePdf(it) }
            return
        }

        pdfPath?.let { path ->
            try {
                val sourceFile = File(path)
                val downloadsDir = context.getExternalFilesDir(null) ?: return
                val fileName = "resume_${_state.value.resume?.id ?: System.currentTimeMillis()}.pdf"
                val destinationFile = File(downloadsDir, fileName)

                sourceFile.copyTo(destinationFile, overwrite = true)

                _state.update { it.copy(pdfGenerationError = null) }

                // Notify user that file has been downloaded
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            destinationFile
                        ), "application/pdf"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _state.update { it.copy(pdfGenerationError = "Failed to download PDF: ${e.localizedMessage}") }
            }
        } ?: run {
            _state.update { it.copy(pdfGenerationError = "PDF file not found") }
        }
    }

    private fun analyzeResume() {
        if (_state.value.resume == null) {
            _state.update { it.copy(analyzeError = "Нет резюме для анализа") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analyzeError = null) }
            try {
                val result = repository.analyzeResume(_state.value.resume!!)
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = null,
                        resume = result
                    )
                }
                // Навигация к экрану анализа после успешного завершения
                onNavigateToAnalysis?.invoke(_state.value.resume!!.id)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = "Не удалось проанализировать резюме: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun applyAiSuggestions() {
        // TODO: Implement applying AI suggestions
        _state.update { it.copy(aiSuggestionsApplied = true) }
    }

    private fun exportResume() {
        // TODO: Implement additional export formats
        _state.value.resume?.let { generatePdf(it) }
    }
} 