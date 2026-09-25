package com.bober.autcsv.domain.model

/**
 * Результат сопоставления резюме с текстом вакансии.
 */
data class VacancyMatch(
    /** Оценка соответствия 0–100. */
    val score: Int = 0,
    /** Ключевые требования, подтверждённые резюме. */
    val matchedKeywords: List<String> = emptyList(),
    /** Ключевые требования, которых не хватает в резюме. */
    val missingKeywords: List<String> = emptyList(),
    /** Краткий вывод на русском: стоит ли откликаться и что усилить. */
    val verdict: String = "",
)
