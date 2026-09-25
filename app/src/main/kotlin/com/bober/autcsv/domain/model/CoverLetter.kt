package com.bober.autcsv.domain.model

/**
 * Сопроводительное письмо, сгенерированное LLM по тексту вакансии
 * для конкретного резюме. Хранится в отдельной таблице БД.
 */
data class CoverLetter(
    val id: String,
    val resumeId: String,
    /** Короткая подпись вакансии (первая строка или заданное название). */
    val vacancyTitle: String,
    /** Полный текст вакансии, по которому письмо сгенерировано. */
    val vacancyText: String,
    /** Текст письма; редактируется пользователем после генерации. */
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
