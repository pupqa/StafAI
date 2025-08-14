package com.bober.autcsv.data.api.llm

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.bober.autcsv.core.constants.LlmConstants
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.Resume
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmService @Inject constructor(
    private val openAI: OpenAI,
    private val gson: Gson,
) {
    suspend fun analyzeResume(resume: Resume): AiAnalysis {
        val prompt = buildAnalysisPrompt(resume)
        val response = getCompletion(prompt)
        return parseAnalysisResponse(response)
    }

    suspend fun suggestImprovements(resume: Resume): List<String> {
        val prompt = buildImprovementPrompt(resume)
        val response = getCompletion(prompt)
        return parseSuggestions(response)
    }

    suspend fun enhanceDescription(text: String): String {
        val prompt = buildEnhancementPrompt(text)
        return getCompletion(prompt)
    }

    private suspend fun getCompletion(prompt: String): String {
        val request = ChatCompletionRequest(
            model = ModelId(OpenRouterConfig.getDefaultModel()),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = LlmConstants.SYSTEM_PROMPT
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            ),
            maxTokens = LlmConstants.MAX_TOKENS,
            temperature = 0.3, // Низкая температура для более консистентных ответов
            topP = 0.9, // Контроль разнообразия
            frequencyPenalty = 0.1, // Штраф за повторения
            presencePenalty = 0.1 // Штраф за повторение тем
        )

        val completion: ChatCompletion = openAI.chatCompletion(request)
        return completion.choices.first().message.content ?: ""
    }

    private fun buildAnalysisPrompt(resume: Resume): String {
        return """
            Проанализируйте это IT-резюме и дайте краткий, точный анализ:
            
            ЛИЧНАЯ ИНФОРМАЦИЯ:
            Имя: ${resume.personalInfo.fullName}
            Специализация: ${resume.personalInfo.specialization}
            Общий опыт: ${resume.personalInfo.totalExperience}
            Опыт по специализации: ${resume.personalInfo.specializationExperience}
            Образование: ${resume.personalInfo.education}
            Иностранные языки: ${resume.personalInfo.languages.joinToString(", ") { "${it.name} (${it.level})" }}
            Контакты: ${resume.personalInfo.email}, ${resume.personalInfo.phone}
            Местоположение: ${resume.personalInfo.location}
            О себе: ${resume.personalInfo.aboutMe}
            
            РЕЗЮМЕ: ${resume.summary}
            
            ПРОФЕССИОНАЛЬНЫЕ НАВЫКИ:
            ОС: ${resume.professionalSkills.operatingSystems.joinToString(", ")}
            Языки программирования: ${resume.professionalSkills.programmingLanguages.joinToString(", ")}
            Фреймворки: ${resume.professionalSkills.frameworks.joinToString(", ")}
            Библиотеки: ${resume.professionalSkills.libraries.joinToString(", ")}
            Базы данных: ${resume.professionalSkills.databases.joinToString(", ")}
            Другие технологии: ${resume.professionalSkills.otherTechnologies.joinToString(", ")}
            Профессиональные достижения: ${resume.professionalSkills.professionalAchievements.joinToString("\n")}
            Сертификации: ${resume.professionalSkills.certifications.joinToString(", ")}
            Мягкие навыки: ${resume.professionalSkills.softSkills.joinToString(", ")}
            
            ПРОЕКТЫ:
            ${resume.projects.joinToString("\n\n") { project ->
                """
                |${project.name} (${project.role}, ${project.duration})
                |Описание: ${project.description}
                |Технологии: ${project.technologies.joinToString(", ")}
                |Обязанности: ${project.responsibilities.joinToString("\n")}
                |Команда: ${project.teamSize}
                """.trimMargin()
            }}
            
            Требования к анализу:
            - Фокусируйтесь на IT-специфичных аспектах
            - Избегайте общих фраз
            - Давайте конкретные, измеримые рекомендации
            - Оценивайте технические навыки и опыт
            - Учитывайте актуальность технологий
            - Анализируйте качество описания проектов
            - Оценивайте логичность структуры резюме
            - Проверяйте конкретность достижений
        """.trimIndent()
    }

    private fun buildImprovementPrompt(resume: Resume): String {
        return """
            Предложите конкретные улучшения для этого IT-резюме:
            
            ${gson.toJson(resume)}
            
            Сосредоточьтесь на:
            1. Технических навыках и их актуальности
            2. Конкретных достижениях и метриках
            3. Проектном опыте и результатах
            4. Современных технологиях и методологиях
            5. Портфолио и примерах кода
            
            Давайте только практические, выполнимые рекомендации.
        """.trimIndent()
    }

    private fun buildEnhancementPrompt(text: String): String {
        return """
            Улучшите это описание для IT-резюме:
            
            $text
            
            Сделайте его:
            1. Более конкретным с техническими деталями
            2. Измеримым с метриками и результатами
            3. Актуальным для IT-рынка
            4. Кратким и информативным
            5. Сфокусированным на достижениях
            
            Избегайте общих фраз и маркетинговых клише.
        """.trimIndent()
    }

    private fun parseAnalysisResponse(response: String): AiAnalysis {
        return try {
            gson.fromJson(response, AiAnalysis::class.java)
        } catch (e: Exception) {
            AiAnalysis(
                completenessScore = 0,
                suggestedImprovements = listOf("Ошибка парсинга ответа ИИ"),
                lastAnalyzed = System.currentTimeMillis()
            )
        }
    }

    private fun parseSuggestions(response: String): List<String> {
        return response.split("\n").filter { it.isNotBlank() }
    }
} 