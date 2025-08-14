package com.bober.autcsv.domain.model

import com.bober.autcsv.core.utils.LlmLogger

data class CvAnalysis(
    val rating: Int,
    val completenessAnalysis: String,
    val logicAnalysis: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendations: List<String>,
    val completenessScore: Int = 0,
    val additionalParameters: Map<String, String> = emptyMap(),
) {
    companion object {
        fun fromLlmResponse(content: String): CvAnalysis {
            LlmLogger.logAnalysisResult("Raw LLM response", "Length: ${content.length}")

            // Extract rating (1-5 stars)
            val rating = content.count { it == '★' }
            LlmLogger.logAnalysisResult("Extracted rating", "$rating stars")

            // More flexible parsing approach
            fun extractSection(keywords: List<String>): String {
                for (keyword in keywords) {
                    val lines = content.lines()
                    val startIndex = lines.indexOfFirst { it.contains(keyword, ignoreCase = true) }
                    if (startIndex != -1) {
                        val result = lines.drop(startIndex + 1)
                            .takeWhile { line ->
                                !line.startsWith("**") &&
                                        !line.matches(Regex("""^\d+\.""")) &&
                                        !line.startsWith("-") &&
                                        !line.startsWith("•") &&
                                        line.isNotBlank()
                            }
                            .joinToString("\n")
                            .trim()
                        LlmLogger.logAnalysisResult(
                            "Extracted section for '$keyword'",
                            "Length: ${result.length}"
                        )
                        return result
                    }
                }
                LlmLogger.logAnalysisResult(
                    "No section found for keywords",
                    keywords.joinToString(", ")
                )
                return ""
            }

            fun extractList(keywords: List<String>): List<String> {
                for (keyword in keywords) {
                    val lines = content.lines()
                    val startIndex = lines.indexOfFirst { it.contains(keyword, ignoreCase = true) }
                    if (startIndex != -1) {
                        val result = lines.drop(startIndex + 1)
                            .takeWhile { line ->
                                !line.startsWith("**") &&
                                        (line.startsWith("-") || line.matches(Regex("""^\d+\.""")) || line.startsWith(
                                            "•"
                                        ))
                            }
                            .map { line ->
                                line.replace(Regex("""^[-\d.•]\s*"""), "").trim()
                            }
                            .filter { it.isNotEmpty() }
                        LlmLogger.logAnalysisResult(
                            "Extracted list for '$keyword'",
                            "Items: ${result.size}"
                        )
                        return result
                    }
                }
                LlmLogger.logAnalysisResult(
                    "No list found for keywords",
                    keywords.joinToString(", ")
                )
                return emptyList()
            }

            // Extract completeness analysis (rating section)
            val completenessAnalysis = extractSection(
                listOf(
                    "Общая оценка", "Overall Assessment", "Оценка полноты", "Rating", "1."
                )
            )

            // Extract logic analysis (improvements section)
            val logicAnalysis = extractSection(
                listOf(
                    "Зоны для улучшения",
                    "Areas for Improvement",
                    "Слабые стороны",
                    "Weak Areas",
                    "3."
                )
            )

            // Extract strengths
            val strengths = extractList(
                listOf(
                    "Сильные стороны", "Strengths", "Сильные стороны:", "Strengths:", "2."
                )
            )

            // Extract improvements
            val improvements = extractList(
                listOf(
                    "Зоны для улучшения",
                    "Areas for Improvement",
                    "Слабые стороны",
                    "Weak Areas",
                    "3."
                )
            )

            // Extract recommendations
            val recommendations = extractList(
                listOf(
                    "Рекомендации",
                    "Recommendations",
                    "Конкретные рекомендации",
                    "Specific recommendations",
                    "4."
                )
            )

            // Extract completeness score (0-100%)
            val completenessScore = extractCompletenessScore(content)

            // Extract additional parameters
            val additionalParameters = extractAdditionalParameters(content)

            // Fallback: if no structured data found, try to extract from raw content
            val fallbackCompleteness = if (completenessAnalysis.isBlank()) {
                content.lines()
                    .takeWhile {
                        !it.contains("сильные стороны", ignoreCase = true) && !it.contains(
                            "strengths",
                            ignoreCase = true
                        )
                    }
                    .joinToString("\n")
                    .trim()
            } else completenessAnalysis

            val fallbackLogic = if (logicAnalysis.isBlank()) {
                content.lines()
                    .dropWhile {
                        !it.contains(
                            "зоны для улучшения",
                            ignoreCase = true
                        ) && !it.contains("areas for improvement", ignoreCase = true)
                    }
                    .takeWhile {
                        !it.contains(
                            "рекомендации",
                            ignoreCase = true
                        ) && !it.contains("recommendations", ignoreCase = true)
                    }
                    .joinToString("\n")
                    .trim()
            } else logicAnalysis

            val result = CvAnalysis(
                rating = if (rating > 0) rating else 3, // Default rating if no stars found
                completenessAnalysis = fallbackCompleteness,
                logicAnalysis = fallbackLogic,
                strengths = strengths,
                improvements = improvements,
                recommendations = recommendations,
                completenessScore = completenessScore,
                additionalParameters = additionalParameters
            )

            LlmLogger.logAnalysisResult(
                "Parsed CvAnalysis",
                "Rating: ${result.rating}, Completeness: ${result.completenessAnalysis.length}, Logic: ${result.logicAnalysis.length}, Strengths: ${result.strengths.size}, Improvements: ${result.improvements.size}, Recommendations: ${result.recommendations.size}, CompletenessScore: ${result.completenessScore}"
            )

            return result
        }

        private fun extractCompletenessScore(content: String): Int {
            val regex = Regex("""(\d{1,3})\s*%""")
            val match = regex.find(content)
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

        private fun extractAdditionalParameters(content: String): Map<String, String> {
            val parameters = mutableMapOf<String, String>()

            val paramKeywords = mapOf(
                "Актуальность технологий" to listOf("актуальность", "технологии", "современность"),
                "Соответствие специализации" to listOf(
                    "соответствие",
                    "специализация",
                    "релевантность"
                ),
                "Качество описания проектов" to listOf("качество", "описание", "проекты"),
                "Логичность структуры" to listOf("логичность", "структура", "организация"),
                "Конкретность достижений" to listOf("конкретность", "достижения", "результаты")
            )

            for ((paramName, keywords) in paramKeywords) {
                for (keyword in keywords) {
                    val lines = content.lines()
                    val lineIndex = lines.indexOfFirst { it.contains(keyword, ignoreCase = true) }
                    if (lineIndex != -1) {
                        val line = lines[lineIndex]
                        val value = line.substringAfter(":").trim()
                        if (value.isNotEmpty()) {
                            parameters[paramName] = value
                            break
                        }
                    }
                }
            }

            return parameters
        }
    }
} 