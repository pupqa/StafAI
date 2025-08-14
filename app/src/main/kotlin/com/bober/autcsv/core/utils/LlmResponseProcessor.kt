package com.bober.autcsv.core.utils

import com.bober.autcsv.domain.model.CvAnalysis
import com.google.gson.Gson
import com.google.gson.JsonObject

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

    private fun countGenericPhrases(text: String): Int {
        val genericPhrases = listOf(
            "важно отметить", "следует отметить", "необходимо отметить",
            "в целом", "в общем", "как правило", "обычно"
        )

        return genericPhrases.count { phrase ->
            text.contains(phrase, ignoreCase = true)
        }
    }

    /**
     * Парсит очищенный JSON-ответ от LLM в доменную модель [CvAnalysis].
     * Используем безопасный парсинг через Gson, без статических методов JsonParser,
     * чтобы избежать несовместимостей версий.
     */
    fun parseJson(cleanedContent: String): CvAnalysis {
        val stripped = cleanedContent
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Попробуем извлечь первый валидный JSON-объект из строки (если вокруг есть текст)
        val candidate = extractFirstJsonObject(stripped) ?: stripped

        val json: JsonObject =
            // 1) Прямая попытка парсинга объекта
            runCatching { Gson().fromJson(candidate, JsonObject::class.java) }
                .getOrNull()
            // 2) Если вернулся null/пусто, попробуем распарсить как строку, содержащую JSON
                ?: run {
                    val inner =
                        runCatching { Gson().fromJson(candidate, String::class.java) }.getOrNull()
                    if (!inner.isNullOrBlank()) runCatching {
                        Gson().fromJson(
                            inner,
                            JsonObject::class.java
                        )
                    }.getOrNull() else null
                }
                // 3) Последняя попытка: если исходный текст содержит JSON-объект, вырезаем его и парсим
                ?: run {
                    val fallback = extractFirstJsonObject(candidate)
                    if (fallback != null) runCatching {
                        Gson().fromJson(
                            fallback,
                            JsonObject::class.java
                        )
                    }.getOrNull() else null
                }
                ?: JsonObject()

        fun JsonObject.optStringArray(name: String): List<String> =
            if (has(name) && get(name).isJsonArray) getAsJsonArray(name).map { el ->
                runCatching { el.asString }.getOrDefault(el.toString())
            } else emptyList()

        fun JsonObject.optInt(name: String): Int =
            if (has(name) && get(name).isJsonPrimitive) runCatching { get(name).asInt }.getOrDefault(
                0
            ) else 0

        fun JsonObject.optString(name: String): String =
            if (has(name) && get(name).isJsonPrimitive) runCatching { get(name).asString }.getOrDefault(
                ""
            ) else ""

        val rating = json.optInt("rating")
        val completenessAnalysis = json.optString("completenessAnalysis")
        val logicAnalysis = json.optString("logicAnalysis")
        val strengths = json.optStringArray("strengths")
        val improvements = json.optStringArray("improvements")
        val recommendations = json.optStringArray("recommendations")
        val completenessScore = json.optInt("completenessScore")

        val additionalParameters =
            if (json.has("additionalParameters") && json.get("additionalParameters").isJsonObject) {
                val obj = json.getAsJsonObject("additionalParameters")
                obj.entrySet().associate { entry ->
                    val value = entry.value
                    entry.key to runCatching { value.asString }.getOrDefault(value.toString())
                }
            } else emptyMap()

        return CvAnalysis(
            rating = rating,
            completenessAnalysis = completenessAnalysis,
            logicAnalysis = logicAnalysis,
            strengths = strengths,
            improvements = improvements,
            recommendations = recommendations,
            completenessScore = completenessScore,
            additionalParameters = additionalParameters
        )
    }

    /**
     * Извлекает первый валидный JSON-объект {...} с учётом кавычек и экранирования.
     */
    private fun extractFirstJsonObject(text: String): String? {
        var inString = false
        var escaped = false
        var depth = 0
        var start = -1
        for (i in text.indices) {
            val c = text[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }

                '}' -> {
                    if (depth > 0) depth--
                    if (depth == 0 && start != -1) {
                        return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }
}

data class ResponseQuality(
    val wordCount: Int,
    val repetitionRatio: Double,
    val genericPhraseCount: Int,
    val isGoodQuality: Boolean,
) 