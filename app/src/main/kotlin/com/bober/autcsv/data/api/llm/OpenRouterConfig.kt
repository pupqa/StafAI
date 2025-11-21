package com.bober.autcsv.data.api.llm

/**
 * Конфигурация доступных моделей OpenRouter и выбор модели по умолчанию.
 */
object OpenRouterConfig {
    // Доступные модели (от лучшей к худшей)
    const val CLAUDE_3_5_SONNET = "anthropic/claude-3-5-sonnet"
    const val CLAUDE_3_5_HAIKU = "anthropic/claude-3-5-haiku"
    const val GPT_4O = "openai/gpt-4o"
    const val GPT_4O_MINI = "openai/gpt-4o-mini"
    const val MISTRAL_LARGE = "qwen/qwen-1.7b-chat"
    const val MISTRAL_MEDIUM = "mistralai/mistral-nemo"
    const val MISTRAL_SMALL = "mistralai/mistral-large-2407"
    const val OPENCHAT = "openchat/openchat-3.5-1210"        // ✅ бесплатная, хороша на русском
    const val GEMMA= "google/gemma-7b-it"                   // ✅ бесплатная
    const val PHI = "microsoft/phi-3-mini-128k-instruct"   // ✅ бесплатная
    const val QWEN_4B = "qwen/qwen-1.5-4b-chat"
    const val QWEN_18B = "qwen/qwen-1.5-1.8b-chat"               // ✅ правильный ID для Qwen 1.8B


    // Используем Claude 3.5 Sonnet как основную модель (лучшее качество)
    private const val DEFAULT_MODEL = CLAUDE_3_5_SONNET

    /** Модель по умолчанию. */
    fun getDefaultModel(): String = DEFAULT_MODEL

    // Функция для получения модели в зависимости от доступности
    /**
     * В перспективе может учитывать доступность/кредиты. Пока возвращает дефолтную.
     */
    fun getModelByPriority(): String {
        return DEFAULT_MODEL
    }
} 