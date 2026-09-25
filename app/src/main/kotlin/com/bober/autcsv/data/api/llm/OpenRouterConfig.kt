package com.bober.autcsv.data.api.llm

/**
 * Конфигурация доступных моделей OpenRouter; порядок перебора при фолбэке
 * задаётся в [com.bober.autcsv.data.repository.LlmRepositoryImpl] и
 * [OpenRouterService].
 */
object OpenRouterConfig {
    // Доступные модели (от лучшей к худшей)
    const val CLAUDE_3_5_SONNET = "anthropic/claude-3.5-sonnet"
    const val CLAUDE_3_5_HAIKU = "anthropic/claude-3.5-haiku"
    const val GPT_4O = "openai/gpt-4o"
    const val GPT_4O_MINI = "openai/gpt-4o-mini"
    const val MISTRAL_LARGE = "mistralai/mistral-large-2407"
    const val MISTRAL_MEDIUM = "mistralai/mistral-nemo"
    const val MISTRAL_SMALL = "mistralai/mistral-small-2409"
    const val LLAMA_8B = "meta-llama/llama-3.1-8b-instruct"     // дешёвая, хороша на русском
    const val GEMMA = "google/gemma-2-9b-it"                    // дешёвая
    const val QWEN_7B = "qwen/qwen-2.5-7b-instruct"
    const val QWEN_14B = "qwen/qwen-2.5-14b-instruct"
}
