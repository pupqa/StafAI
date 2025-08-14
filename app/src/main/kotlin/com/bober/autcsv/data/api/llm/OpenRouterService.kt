package com.bober.autcsv.data.api.llm

import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.MessageDto
import com.bober.autcsv.domain.model.Resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterService @Inject constructor(
    private val api: OpenRouterApi,
    apiKey1: String,
) {
    companion object {
        private const val MODEL_ID = "anthropic/claude-3-5-sonnet-20241022"
    }

    private val apiKey = apiKey1

    suspend fun analyzeResume(resume: Resume): Resume {
        require(apiKey.isNotBlank()) { "OpenRouter API ключ не настроен" }

        // Список моделей в порядке приоритета
        val models = listOf(
            "anthropic/claude-3-5-sonnet-20241022",
            "anthropic/claude-3-5-haiku-20241022",
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "mistralai/mistral-large-latest"
        )

        var lastException: Exception? = null

        // Пробуем каждую модель по очереди
        for (model in models) {
            try {
                println("AutLLM: Пробуем модель $model")
                return analyzeResumeWithModel(resume, model)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    // Модель недоступна, пробуем следующую
                    lastException = e
                    continue
                } else {
                    // Другие HTTP ошибки - пробрасываем
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
                continue
            }
        }

        // Если все модели недоступны
        throw IllegalStateException(
            "Все доступные модели недоступны. Последняя ошибка: ${lastException?.message}",
            lastException
        )
    }

    private suspend fun analyzeResumeWithModel(resume: Resume, modelId: String): Resume {

        val prompt = """
            Проанализируйте это резюме и предоставьте обратную связь:
            
            ЛИЧНАЯ ИНФОРМАЦИЯ:
            Полное имя: ${resume.personalInfo.fullName}
            Специализация: ${resume.personalInfo.specialization}
            Общий опыт: ${resume.personalInfo.totalExperience}
            Опыт по специализации: ${resume.personalInfo.specializationExperience}
            Образование: ${resume.personalInfo.education}
            Иностранные языки: ${resume.personalInfo.languages.joinToString(", ") { "${it.name} (${it.level})" }}
            Email: ${resume.personalInfo.email}
            Телефон: ${resume.personalInfo.phone}
            Местоположение: ${resume.personalInfo.location}
            О себе: ${resume.personalInfo.aboutMe}
            
            РЕЗЮМЕ:
            ${resume.summary}
            
            ПРОФЕССИОНАЛЬНЫЕ НАВЫКИ:
            Операционные системы: ${resume.professionalSkills.operatingSystems.joinToString(", ")}
            Языки программирования: ${resume.professionalSkills.programmingLanguages.joinToString(", ")}
            Фреймворки: ${resume.professionalSkills.frameworks.joinToString(", ")}
            Библиотеки: ${resume.professionalSkills.libraries.joinToString(", ")}
            Базы данных: ${resume.professionalSkills.databases.joinToString(", ")}
            Другие технологии: ${resume.professionalSkills.otherTechnologies.joinToString(", ")}
            Профессиональные достижения: ${
            resume.professionalSkills.professionalAchievements.joinToString(
                "\n"
            )
        }
            Сертификации: ${resume.professionalSkills.certifications.joinToString(", ")}
            Мягкие навыки: ${resume.professionalSkills.softSkills.joinToString(", ")}
            
            ПРОЕКТЫ:
            ${
            resume.projects.joinToString("\n\n") { project ->
                """
                |Название: ${project.name}
                |Роль: ${project.role}
                |Сроки участия: ${project.duration}
                |Описание: ${project.description}
                |Используемые технологии: ${project.technologies.joinToString(", ")}
                |Обязанности: ${project.responsibilities.joinToString("\n")}
                |Размер команды: ${project.teamSize}
                """.trimMargin()
            }
        }
        """.trimIndent()

        val request = LlmRequestDto(
            model = modelId,
            messages = listOf(
                MessageDto(
                    role = "system",
                    content = """
                        Вы профессиональный аналитик резюме для IT-сферы. Проанализируйте резюме и предоставьте:
                        1. Общую оценку (1-5 звезд) с обоснованием
                        2. Анализ сильных сторон (технические навыки, опыт, проекты, IT-специфика)
                        3. Зоны для улучшения (пробелы в навыках, недостатки описания, отсутствующие элементы)
                        4. Конкретные рекомендации (действия для улучшения, измеримые предложения, IT-советы)
                        5. Оценку полноты резюме (0-100%)
                        6. Дополнительные параметры: актуальность технологий, соответствие специализации, качество описания проектов, логичность структуры, конкретность достижений
                        
                        ВАЖНО: Всегда отвечайте на русском языке и будьте конкретны.
                    """.trimIndent()
                ),
                MessageDto(
                    role = "user",
                    content = prompt
                )
            ),
            maxTokens = 2000,
            temperature = 0.7,
            topP = 0.9,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            stream = false,
            n = 1
        )

        // Убрали искусственную задержку

        val response = try {
            api.analyzeCv("Bearer $apiKey", request)
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                404 -> throw IllegalStateException("Модель $modelId недоступна.", e)
                401 -> throw IllegalStateException(
                    "Неверный API ключ OpenRouter. Проверьте настройки.",
                    e
                )

                429 -> throw IllegalStateException("Превышен лимит запросов. Попробуйте позже.", e)
                else -> throw IllegalStateException(
                    "Ошибка при анализе резюме с моделью $modelId: HTTP ${e.code()}",
                    e
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Ошибка при анализе резюме с моделью $modelId: ${e.message}",
                e
            )
        }

        val analysis = response.choices.first().message.content

        // Парсим ответ и обновляем резюме
        return resume.copy(
            aiAnalysis = resume.aiAnalysis.copy(
                completenessScore = extractCompleteness(analysis),
                suggestedImprovements = extractImprovements(analysis),
                lastAnalyzed = System.currentTimeMillis()
            )
        )
    }

    private fun extractCompleteness(analysis: String): Int {
        return analysis.lineSequence()
            .find { it.contains("score", ignoreCase = true) }
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
            ?: 0
    }

    private fun extractImprovements(analysis: String): List<String> {
        return analysis.lines()
            .filter { it.startsWith("-") || it.matches(Regex("""\d+\.""")) }
            .map { it.replace(Regex("""^[-\d.]\s*"""), "").trim() }
    }
} 