package com.bober.autcsv.domain.usecase

import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.CvAnalysis
import com.bober.autcsv.domain.repository.LlmRepository
import javax.inject.Inject

/**
 * Запускает реальный LLM-анализ текста резюме через [LlmRepository]:
 * репозиторий сам выбирает модель (фолбэк по цепочке), объём токенов
 * и парсит строгий JSON-ответ в [CvAnalysis].
 */
class AnalyzeCvUseCase @Inject constructor(
    private val repository: LlmRepository,
) {
    suspend operator fun invoke(cvContent: String): Result<CvAnalysis> {
        LlmLogger.logUiEvent(
            "UseCase",
            "AnalyzeCvUseCase invoked",
            "Content length: ${cvContent.length}"
        )
        val startTime = System.currentTimeMillis()

        return try {
            val result = repository.analyzeCV(cvContent)

            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("AnalyzeCvUseCase", duration)

            result.onSuccess { analysis ->
                LlmLogger.logAnalysisResult("UseCase", "Анализ успешно завершен")
                LlmLogger.logAnalysisResult(
                    "UseCase",
                    "Completeness: ${analysis.completenessScore}%, " +
                            "Strengths: ${analysis.strengths.size}, " +
                            "Recommendations: ${analysis.recommendations.size}"
                )
            }.onFailure { error ->
                LlmLogger.logError("AnalyzeCvUseCase завершился с ошибкой", error)
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LlmLogger.logPerformance("AnalyzeCvUseCase (exception)", duration)
            LlmLogger.logError("AnalyzeCvUseCase exception", e)
            Result.failure(e)
        }
    }
}
