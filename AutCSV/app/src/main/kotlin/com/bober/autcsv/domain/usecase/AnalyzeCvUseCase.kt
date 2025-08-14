package com.bober.autcsv.domain.usecase

import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.CvAnalysis
import com.bober.autcsv.domain.repository.LlmRepository
import javax.inject.Inject

/**
 * Use case для анализа резюме (CV) с помощью LLM.
 *
 * Отвечает за оркестрацию вызова репозитория, базовое логирование
 * пользовательских событий и метрик производительности, а также
 * нормализацию результата в виде [Result] с доменной моделью [CvAnalysis].
 *
 * Архитектурно располагается на слое domain и инкапсулирует бизнес-правила:
 * - валидация входных данных делегируется нижележащим слоям,
 * - обработка ошибок производится через Result и централизованное логирование.
 */
class AnalyzeCvUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    /**
     * Запускает анализ переданного текстового содержимого резюме.
     *
     * Процедура:
     * 1. Логирует факт запуска и длину входного текста.
     * 2. Делегирует анализ репозиторию [LlmRepository].
     * 3. Фиксирует и логирует длительность операции.
     * 4. Возвращает [Result] с [CvAnalysis] при успехе или с ошибкой при неудаче.
     *
     * @param cvContent Текстовая версия резюме (plain text), подготовленная для анализа LLM.
     * @return [Result] содержащий доменную модель анализа [CvAnalysis] либо ошибку.
     */
    suspend operator fun invoke(cvContent: String): Result<CvAnalysis> {
        LlmLogger.logUiEvent("UseCase", "AnalyzeCvUseCase invoked", "Content length: ${cvContent.length}")
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