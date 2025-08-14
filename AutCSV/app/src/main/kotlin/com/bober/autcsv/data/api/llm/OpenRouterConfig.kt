package com.bober.autcsv.data.api.llm

object OpenRouterConfig {
    // Доступные модели (от лучшей к худшей)
    const val CLAUDE_3_5_SONNET = "anthropic/claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU = "anthropic/claude-3-5-haiku-20241022"
    const val GPT_4O = "openai/gpt-4o"
    const val GPT_4O_MINI = "openai/gpt-4o-mini"
    const val MISTRAL_LARGE = "mistralai/mistral-large-latest"
    const val MISTRAL_MEDIUM = "mistralai/mistral-medium-latest"
    const val MISTRAL_SMALL = "mistralai/mistral-small-latest"
    
    // Используем Claude 3.5 Sonnet как основную модель (лучшее качество)
    private const val DEFAULT_MODEL = CLAUDE_3_5_SONNET
    
    fun getDefaultModel(): String = DEFAULT_MODEL
    
    // Функция для получения модели в зависимости от доступности
    fun getModelByPriority(): String {
        return DEFAULT_MODEL
    }
} 