package com.bober.autcsv.data.api.llm

import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.LlmResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API для работы с OpenRouter чат-комплишнами.
 */
interface OpenRouterApi {
    /**
     * Выполняет чат-запрос к LLM для анализа резюме.
     * @param authorization Заголовок авторизации вида "Bearer <token>".
     * @param request Тело запроса с моделью и сообщениями.
     */
    @POST("chat/completions")
    suspend fun analyzeCv(
        @Header("Authorization") authorization: String,
        @Body request: LlmRequestDto
    ): LlmResponseDto
} 