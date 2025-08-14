package com.bober.autcsv.domain.repository

import com.bober.autcsv.domain.model.CvAnalysis
import kotlin.Result

/**
 * Контракт репозитория для взаимодействия со службами LLM.
 *
 * Слой domain определяет абстракцию, не зависящую от конкретной реализации
 * сетевого клиента, провайдера моделей и схемы сериализации.
 */
interface LlmRepository {
    /**
     * Выполняет анализ содержимого резюме с помощью LLM.
     *
     * Реализация должна:
     * - корректно обрабатывать сетевые ошибки и таймауты,
     * - учитывать ограничения моделей (токены, приоритетность),
     * - возвращать доменную модель [CvAnalysis] в случае успеха.
     *
     * @param cvContent Текст резюме для анализа.
     * @return [Result] с [CvAnalysis] при успехе или с причиной ошибки при неудаче.
     */
    suspend fun analyzeCV(cvContent: String): Result<CvAnalysis>
} 