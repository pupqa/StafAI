package com.bober.autcsv.core.utils

/**
 * Утилиты для пост-обработки и оценки качества ответов LLM.
 * Содержит функции очистки текста и простые метрики качества результата.
 */
object LlmResponseProcessor {
    
    /**
     * Очищает ответ от повторений и нерелевантного контента
     */
    fun cleanResponse(response: String): String {
        return response
            .removeCommonRepetitions()
            .removeGenericPhrases()
            .removeMarkdownArtifacts()
            .trim()
    }
    
    /**
     * Эвристически удаляет повторяющиеся строки.
     */
    private fun String.removeCommonRepetitions(): String {
        val lines = this.lines()
        val uniqueLines = mutableListOf<String>()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !uniqueLines.contains(trimmedLine)) {
                uniqueLines.add(trimmedLine)
            }
        }
        
        return uniqueLines.joinToString("\n")
    }
    
    /**
     * Удаляет «общие фразы», которые засоряют текст и не несут ценности.
     */
    private fun String.removeGenericPhrases(): String {
        val genericPhrases = listOf(
            "важно отметить",
            "следует отметить",
            "необходимо отметить",
            "стоит отметить",
            "важно помнить",
            "следует помнить",
            "необходимо помнить",
            "стоит помнить",
            "в целом",
            "в общем",
            "в принципе",
            "как правило",
            "обычно",
            "традиционно",
            "стандартно",
            "типично",
            "характерно",
            "свойственно",
            "присуще",
            "свойственно для",
            "характерно для",
            "присуще для"
        )
        
        var result = this
        for (phrase in genericPhrases) {
            result = result.replace(Regex(phrase, RegexOption.IGNORE_CASE), "")
        }
        
        return result
    }
    
    /**
     * Убирает основные артефакты Markdown, чтобы получить чистый текст.
     */
    private fun String.removeMarkdownArtifacts(): String {
        return this
            .replace(Regex("```[\\w]*\\n"), "") // Убираем блоки кода
            .replace(Regex("```"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Убираем жирный текст
            .replace(Regex("\\*([^*]+)\\*"), "$1") // Убираем курсив
            .replace(Regex("`([^`]+)`"), "$1") // Убираем inline код
            .replace(Regex("\\n\\s*\\n\\s*\\n"), "\n\n") // Убираем лишние переносы
    }
    
    /**
     * Проверяет качество ответа
     */
    fun validateResponseQuality(response: String): ResponseQuality {
        val lines = response.lines()
        val wordCount = response.split("\\s+".toRegex()).size
        
        // Проверяем на повторения
        val uniqueLines = lines.toSet()
        val repetitionRatio = if (lines.isNotEmpty()) {
            (lines.size - uniqueLines.size) / lines.size.toDouble()
        } else 0.0
        
        // Проверяем на общие фразы
        val genericPhraseCount = countGenericPhrases(response)
        
        return ResponseQuality(
            wordCount = wordCount,
            repetitionRatio = repetitionRatio,
            genericPhraseCount = genericPhraseCount,
            isGoodQuality = repetitionRatio < 0.3 && genericPhraseCount < 5 && wordCount > 50
        )
    }
    
    /**
     * Подсчитывает количество «общих фраз» в тексте для метрики качества.
     */
    private fun countGenericPhrases(text: String): Int {
        val genericPhrases = listOf(
            "важно отметить", "следует отметить", "необходимо отметить",
            "в целом", "в общем", "как правило", "обычно"
        )
        
        return genericPhrases.count { phrase ->
            text.contains(phrase, ignoreCase = true)
        }
    }
}

/**
 * Метрики качества ответа LLM.
 */
data class ResponseQuality(
    val wordCount: Int,
    val repetitionRatio: Double,
    val genericPhraseCount: Int,
    val isGoodQuality: Boolean
) 