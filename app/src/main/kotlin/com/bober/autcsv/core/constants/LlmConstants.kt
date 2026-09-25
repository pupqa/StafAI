package com.bober.autcsv.core.constants

import com.bober.autcsv.core.constants.LlmConstants.systemPrompt


/**
 * Константы взаимодействия с LLM (OpenRouter): базовый URL и системный промпт.
 * Параметры генерации задаются в местах построения запросов.
 */
object LlmConstants {
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"

    /**
     * Системный промпт для строгого JSON-ответа на русском языке.
     * Описывает структуру JSON и требования к содержанию. Язык ответа
     * подставляется в [systemPrompt] по языку интерфейса приложения.
     */
    private val SYSTEM_PROMPT_TEMPLATE = """
        Вы — эксперт по анализу резюме для IT-сферы. %LANGUAGE%
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
          "additionalParameters": {         // 2-5 параметров оценки; имена и значения — НА ЯЗЫКЕ ОТВЕТА,
            "<название параметра>": string  // например «Актуальность технологий»: «высокая»
          }
        }

        Требования к содержанию:
        - Будьте конкретны и избегайте общих фраз.
        - Ссылайтесь на факты из резюме (навыки, опыт, проекты).
        - Максимальная практичность рекомендаций.
    """.trimIndent()

    const val RESPONSE_LANGUAGE_RU = "Отвечайте ТОЛЬКО на русском языке."
    const val RESPONSE_LANGUAGE_EN = "Respond ONLY in English."

    /** Системный промпт с директивой языка ответа. */
    fun systemPrompt(respondInEnglish: Boolean): String =
        SYSTEM_PROMPT_TEMPLATE.replace(
            "%LANGUAGE%",
            if (respondInEnglish) RESPONSE_LANGUAGE_EN else RESPONSE_LANGUAGE_RU
        )
}