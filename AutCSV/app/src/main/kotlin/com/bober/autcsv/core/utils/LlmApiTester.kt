package com.bober.autcsv.core.utils

import com.bober.autcsv.data.api.llm.OpenRouterApi
import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmApiTester @Inject constructor(
    private val api: OpenRouterApi
) {
    
    suspend fun testModelAvailability(modelId: String, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = LlmRequestDto(
                    model = modelId,
                    messages = listOf(
                        MessageDto(
                            role = "user",
                            content = "Тест доступности модели"
                        )
                    ),
                    maxTokens = 10,
                    temperature = 0.1,
                    topP = 0.9,
                    frequencyPenalty = 0.0,
                    presencePenalty = 0.0,
                    stream = false,
                    n = 1
                )
                
                api.analyzeCv("Bearer $apiKey", request)
                true
            } catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    404 -> {
                        println("AutLLM: Модель $modelId недоступна (404)")
                        false
                    }
                    401 -> {
                        println("AutLLM: Неверный API ключ (401)")
                        false
                    }
                    else -> {
                        println("AutLLM: Ошибка при тестировании модели $modelId: HTTP ${e.code()}")
                        false
                    }
                }
            } catch (e: Exception) {
                println("AutLLM: Ошибка при тестировании модели $modelId: ${e.message}")
                false
            }
        }
    }
    
    suspend fun findAvailableModels(apiKey: String): List<String> {
        val modelsToTest = listOf(
            "anthropic/claude-3-5-sonnet-20241022",
            "anthropic/claude-3-5-haiku-20241022",
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "mistralai/mistral-large-latest",
            "mistralai/mistral-medium-latest",
            "mistralai/mistral-small-latest"
        )
        
        val availableModels = mutableListOf<String>()
        
        for (model in modelsToTest) {
            if (testModelAvailability(model, apiKey)) {
                availableModels.add(model)
                println("AutLLM: Модель $model доступна")
            }
        }
        
        return availableModels
    }
} 