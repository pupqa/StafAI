package com.bober.autcsv.core.constants

/**
 * Константы и настройки по умолчанию для взаимодействия с LLM (OpenRouter).
 * Содержит базовый URL, дефолтную модель, параметры генерации и системный промпт.
 */
object LlmConstants {
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"
    const val DEFAULT_MODEL = "anthropic/claude-3-5-sonnet-20241022"
    const val MAX_TOKENS = 1500

    // Настройки качества
    const val DEFAULT_TEMPERATURE = 0.3
    const val DEFAULT_TOP_P = 0.9
    const val DEFAULT_FREQUENCY_PENALTY = 0.1
    const val DEFAULT_PRESENCE_PENALTY = 0.1

    // Настройки для разных типов анализа
    /**
     * Температуры для разных режимов анализа: быстрый, детальный, креативный.
     */
    object AnalysisSettings {
        const val QUICK_ANALYSIS_TEMPERATURE = 0.2
        const val DETAILED_ANALYSIS_TEMPERATURE = 0.4
        const val CREATIVE_ANALYSIS_TEMPERATURE = 0.6
    }

    /**
     * Системный промпт для строгого JSON-ответа на русском языке.
     * Описывает структуру JSON и требования к содержанию.
     */
    val SYSTEM_PROMPT = """
        Вы — эксперт по анализу резюме для IT-сферы. Отвечайте ТОЛЬКО на русском языке.
        Верните СТРОГО валидный JSON без каких-либо префиксов, комментариев и оформлений (без markdown и без подсветки кода).

        СХЕМА JSON ОТВЕТА:
        {
          "rating": number,                 // целое 1..5
          "completenessAnalysis": string,   // краткий анализ полноты
          "logicAnalysis": string,          // краткий анализ логичности структуры
          "strengths": [string],            // список сильных сторон
          "improvements": [string],         // список зон улучшений
          "recommendations": [string],      // конкретные рекомендации
          "completenessScore": number,      // 0..100
          "additionalParameters": {         // произвольные доп. параметры
            "techRelevance": string,
            "specializationFit": string,
            "projectsQuality": string
          }
        }

        Требования к содержанию:
        - Будьте конкретны и избегайте общих фраз.
        - Ссылайтесь на факты из резюме (навыки, опыт, проекты).
        - Максимальная практичность рекомендаций.
    """.trimIndent()
} 