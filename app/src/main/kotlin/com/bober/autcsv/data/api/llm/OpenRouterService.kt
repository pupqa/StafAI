package com.bober.autcsv.data.api.llm

import com.bober.autcsv.core.utils.ResumeTranslator
import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.MessageDto
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.VacancyMatch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис LLM поверх OpenRouter: единый чат-вызов с фолбэком по списку
 * моделей и типизированные сценарии — полный анализ резюме, улучшение
 * текста, генерация «О себе» и сопоставление с вакансией.
 *
 * Бесплатные модели идут первыми, платные — резерв. Ответы для
 * структурированных сценариев запрашиваются строго в JSON и аккуратно
 * разбираются (Gson оставляет отсутствующие поля null — маппим через
 * orEmpty()/коэрсы).
 */
@Singleton
class OpenRouterService @Inject constructor(
    private val api: OpenRouterApi,
    private val apiKeyStore: com.bober.autcsv.core.utils.ApiKeyStore,
    /** Ключ из BuildConfig — резерв, если пользователь не сохранил свой. */
    @javax.inject.Named("openrouter_api_key") private val buildKey: String,
) {
    companion object {
        /** Бесплатные/дешёвые модели идут первыми; платные — резерв. */
        private val MODELS = listOf(
            OpenRouterConfig.LLAMA_8B,
            OpenRouterConfig.GEMMA,
            OpenRouterConfig.QWEN_7B,
            OpenRouterConfig.QWEN_14B,
            OpenRouterConfig.MISTRAL_SMALL,
            OpenRouterConfig.GPT_4O_MINI,
            OpenRouterConfig.CLAUDE_3_5_HAIKU,
            OpenRouterConfig.CLAUDE_3_5_SONNET,
        )
    }

    private val gson = Gson()

    /** Ключ из настроек приоритетнее ключа сборки. */
    private fun currentApiKey(): String =
        apiKeyStore.getUserKey().ifBlank { buildKey }

    /** Вид улучшаемого текста — влияет на формулировку промпта. */
    enum class TextKind(val labelRu: String) {
        ABOUT_ME("раздел «О себе»"),
        PROJECT_DESCRIPTION("описание проекта"),
        ACHIEVEMENT("формулировка достижения"),
        SUMMARY("краткое профессиональное резюме"),
    }

    // ── Ядро: чат с фолбэком моделей ───────────────────────────────────

    private suspend fun chat(
        system: String,
        user: String,
        maxTokens: Int = 1500,
        temperature: Double = 0.4,
    ): String {
        val apiKey = currentApiKey()
        require(apiKey.isNotBlank()) { "OpenRouter API ключ не настроен — укажите его в настройках приложения" }
        var lastError: Exception? = null
        for (model in MODELS) {
            try {
                val request = LlmRequestDto(
                    model = model,
                    messages = listOf(
                        MessageDto(role = "system", content = system),
                        MessageDto(role = "user", content = user),
                    ),
                    maxTokens = maxTokens,
                    temperature = temperature,
                    stream = false,
                )
                val response = api.analyzeCv("Bearer $apiKey", request)
                val content = response.choices.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) return content.trim()
                lastError = IllegalStateException("Пустой ответ от модели $model")
            } catch (e: HttpException) {
                lastError = e
                // 401 — ключ неверный, дальше смысла нет; остальные коды — следующая модель
                if (e.code() == 401) {
                    throw IllegalStateException(
                        "Неверный API ключ OpenRouter. Проверьте настройки.",
                        e
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException(
            "Не удалось получить ответ от LLM: ${lastError?.message ?: "нет доступных моделей"}",
            lastError
        )
    }

    /** Достаёт JSON из ответа модели (часто обёрнут в ```json …```). */
    private fun extractJson(content: String): String {
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(content)
        val raw = fenced?.groupValues?.get(1) ?: content
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw.trim()
    }

    private suspend fun <T> chatJson(
        system: String,
        user: String,
        maxTokens: Int = 1500,
        clazz: Class<T>,
    ): T = gson.fromJson(extractJson(chat(system, user, maxTokens)), clazz)

    // ── Сценарии ───────────────────────────────────────────────────────

    /** Язык ответа модели подбирается под язык интерфейса приложения. */
    private fun responseLanguage(): String =
        when (androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0]?.language) {
            "en" -> "на английском"
            else -> "на русском"
        }

    /** Полный структурированный анализ резюме. */
    suspend fun analyzeResume(resume: Resume): Resume {
        val dto = chatJson(
            system = """
                Ты профессиональный карьерный консультант в IT. Проанализируй резюме и ответь
                СТРОГО валидным JSON без markdown и пояснений по схеме:
                {"score": 0-100, "improvements": ["..."], "strengths": ["..."],
                 "weaknesses": ["..."], "enhanced": {"aboutMe": "улучшенный раздел О себе"},
                 "keywords": ["ключевые слова для ATS"]}
                Отвечай ${responseLanguage()}. improvements — 3-5 конкретных действий; enhanced.aboutMe —
                улучшенная версия раздела «О себе» (3-4 предложения, без воды).
            """.trimIndent(),
            user = resumeToPrompt(resume),
            maxTokens = 2000,
            clazz = AnalysisDto::class.java,
        )
        return resume.copy(
            aiAnalysis = resume.aiAnalysis.copy(
                completenessScore = (dto.score ?: 0).coerceIn(0, 100),
                suggestedImprovements = dto.improvements.orEmpty(),
                strengthAreas = dto.strengths.orEmpty(),
                weakAreas = dto.weaknesses.orEmpty(),
                enhancedDescriptions = dto.enhanced.orEmpty(),
                keywordSuggestions = dto.keywords.orEmpty(),
                lastAnalyzed = System.currentTimeMillis(),
            )
        )
    }

    /** Переписывает текст резюме профессиональнее и конкретнее. */
    suspend fun improveText(text: String, kind: TextKind): String {
        val improved = chat(
            system = """
                Ты профессиональный редактор резюме. Перепиши ${kind.labelRu}: конкретно,
                с измеримыми результатами, без клише и воды, в 2–4 предложениях.
                Сохрани факты и технологии из исходного текста. Пиши ${responseLanguage()}.
                В ответе — только итоговый текст, без кавычек и пояснений.
            """.trimIndent(),
            user = text,
            maxTokens = 600,
        )
        return improved.removeSurrounding("\"").trim()
    }

    /** Генерирует раздел «О себе» по данным резюме. */
    suspend fun generateAboutMe(resume: Resume): String {
        val improved = chat(
            system = """
                Ты карьерный консультант. Напиши раздел «О себе» для резюме на основе данных:
                3–4 предложения, первое — роль и стаж, затем сильные стороны и ключевой стек,
                в конце — чем полезен работодателю. Без клише вроде «стрессоустойчивый».
                Пиши ${responseLanguage()}. В ответе — только итоговый текст.
            """.trimIndent(),
            user = resumeToPrompt(resume),
            maxTokens = 600,
        )
        return improved.removeSurrounding("\"").trim()
    }

    /** Сопоставляет резюме с текстом вакансии. */
    suspend fun matchVacancy(resume: Resume, vacancyText: String): VacancyMatch {
        val dto = chatJson(
            system = """
                Ты технический рекрутер. Сравни резюме кандидата с текстом вакансии и ответь
                СТРОГО валидным JSON без markdown:
                {"score": 0-100, "matched": ["требования, которые есть в резюме"],
                 "missing": ["ключевые требования, которых не хватает"], "verdict": "вывод в 1-2 предложениях"}
                score — вероятность прохождения скрининга. Отвечай ${responseLanguage()}.
            """.trimIndent(),
            user = "ВАКАНСИЯ:\n$vacancyText\n\nРЕЗЮМЕ:\n${resumeToPrompt(resume)}",
            maxTokens = 900,
            clazz = MatchDto::class.java,
        )
        return VacancyMatch(
            score = (dto.score ?: 0).coerceIn(0, 100),
            matchedKeywords = dto.matched.orEmpty(),
            missingKeywords = dto.missing.orEmpty(),
            verdict = dto.verdict.orEmpty(),
        )
    }

    /**
     * Сопроводительное письмо от имени кандидата под конкретную вакансию.
     * Возвращает готовый текст письма (plain text) на языке интерфейса.
     */
    suspend fun generateCoverLetter(resume: Resume, vacancyText: String): String {
        val improved = chat(
            system = """
                Ты карьерный консультант. Напиши сопроводительное письмо от имени
                кандидата по его резюме и тексту вакансии.
                Требования: 150–250 слов; деловой тон без канцелярита; 2–3 конкретных
                факта из резюме, закрывающих ключевые требования вакансии; в конце —
                мягкий призыв к действию. Без плейсхолдеров вида [Имя компании].
                Пиши ${responseLanguage()}. В ответе — только текст письма.
            """.trimIndent(),
            user = "ВАКАНСИЯ:\n$vacancyText\n\nРЕЗЮМЕ КАНДИДАТА:\n${resumeToPrompt(resume)}",
            maxTokens = 900,
            temperature = 0.5,
        )
        return improved.removeSurrounding("\"").trim()
    }

    /**
     * Перевод резюме на целевой язык («ru»/«en»): свободные текстовые поля
     * переводятся моделью, структура и нетекстовые данные сохраняются.
     */
    suspend fun translateResume(resume: Resume, targetLanguageCode: String): Resume {
        val fields = ResumeTranslator.extractTranslatable(resume)
        if (fields.isEmpty()) return resume

        val targetLanguageName =
            if (targetLanguageCode.startsWith("ru")) "русский" else "английский"
        val payload = fields.associate { it.path to it.value }

        val content = chat(
            system = """
                Ты профессиональный переводчик резюме. Переведи значения полей JSON
                на $targetLanguageName язык для делового резюме.
                Правила: ключи JSON оставь без изменений; технические термины,
                названия продуктов, компаний и ссылки не переводи; стиль — деловой,
                краткий; не добавляй новых полей и комментариев.
                Верни СТРОГО валидный JSON без markdown.
            """.trimIndent(),
            user = Gson().toJson(payload),
            maxTokens = 2000,
            temperature = 0.2,
        )

        val translations: Map<String, String> = Gson().fromJson(
            extractJson(content),
            object : TypeToken<Map<String, String>>() {}.type,
        )
        return ResumeTranslator.applyTranslations(resume, translations)
    }

    // ── Промпт ─────────────────────────────────────────────────────────

    private fun resumeToPrompt(resume: Resume): String = buildString {
        val p = resume.personalInfo
        appendLine("Имя: ${p.fullName}")
        appendLine("Должность: ${p.specialization}")
        appendLine("Опыт: ${p.totalExperience} лет (по специальности: ${p.specializationExperience})")
        appendLine("Город: ${p.location}; Релокация: ${p.readyToRelocate} ${p.relocationCities}")
        appendLine("Желаемая зарплата: ${p.salaryMin}–${p.salaryMax} ₽")
        appendLine("Образование: ${p.education}")
        appendLine("Языки: ${p.languages.joinToString(", ") { "${it.name} (${it.level})" }}")
        appendLine("О себе: ${p.aboutMe}")
        appendLine("Резюме (summary): ${resume.summary}")
        val s = resume.professionalSkills
        appendLine(
            "Навыки: ${
                (listOf(
                    s.programmingLanguages,
                    s.frameworks,
                    s.libraries,
                    s.databases,
                    s.otherTechnologies
                ).flatten() + s.softSkills).joinToString(", ")
            }"
        )
        appendLine("Сертификации: ${s.certifications.joinToString(", ")}")
        appendLine("Достижения: ${s.professionalAchievements.joinToString("; ")}")
        appendLine("Проекты:")
        resume.projects.forEach { pr ->
            appendLine(
                "- ${pr.name} (${pr.role}, ${pr.duration}): ${pr.description} [${
                    pr.technologies.joinToString(
                        ", "
                    )
                }]"
            )
        }
    }

    // ── DTO структурированных ответов ──────────────────────────────────

    private data class AnalysisDto(
        val score: Int? = null,
        val improvements: List<String>? = null,
        val strengths: List<String>? = null,
        val weaknesses: List<String>? = null,
        val enhanced: Map<String, String>? = null,
        val keywords: List<String>? = null,
    )

    private data class MatchDto(
        val score: Int? = null,
        val matched: List<String>? = null,
        val missing: List<String>? = null,
        val verdict: String? = null,
    )
}
