package com.bober.autcsv.domain.usecase

import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.CvAnalysis
import com.bober.autcsv.domain.repository.LlmRepository
import javax.inject.Inject

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