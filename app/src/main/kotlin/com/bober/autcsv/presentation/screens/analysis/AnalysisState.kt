package com.bober.autcsv.presentation.screens.analysis

import com.bober.autcsv.domain.model.CvAnalysis

/**
 * Состояния экрана анализа: начальное, загрузка, успех с [CvAnalysis], ошибка с сообщением.
 */
sealed class AnalysisState {
    data object Initial : AnalysisState()
    data object Loading : AnalysisState()
    data class Success(val analysis: CvAnalysis) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
} 