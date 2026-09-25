package com.bober.autcsv.data.repository

import androidx.appcompat.app.AppCompatDelegate
import com.bober.autcsv.core.constants.LlmConstants
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.LlmResponseProcessor
import com.bober.autcsv.data.api.llm.OpenRouterApi
import com.bober.autcsv.data.api.llm.OpenRouterConfig
import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.MessageDto
import com.bober.autcsv.data.api.llm.dto.ResponseFormatDto
import com.bober.autcsv.domain.model.CvAnalysis
import com.bober.autcsv.domain.repository.LlmRepository
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named
import kotlin.Result

/** Пустой разбор без единого значимого поля — ответ модели не распарсился. */
private fun CvAnalysis.isBlank(): Boolean =
    rating == 0 && completenessScore == 0 &&
            strengths.isEmpty() && improvements.isEmpty() &&
            recommendations.isEmpty() && completenessAnalysis.isBlank() &&
            logicAnalysis.isBlank()

class LlmRepositoryImpl @Inject constructor(
    private val api: OpenRouterApi,
    private val apiKeyStore: com.bober.autcsv.core.utils.ApiKeyStore,
    /** Ключ из BuildConfig — резерв. */
    @Named("openrouter_api_key") private val buildKey: String,
) : LlmRepository {

    /** Пауза перед следующей попыткой после ошибки модели/лимита. */
    private companion object {
        const val RETRY_DELAY_MS = 800L
    }

    // Список моделей в порядке приоритета (от лучшей к худшей)
    private val modelPriority = listOf(
        OpenRouterConfig.CLAUDE_3_5_SONNET,
        OpenRouterConfig.CLAUDE_3_5_HAIKU,
        OpenRouterConfig.GPT_4O,
        OpenRouterConfig.GPT_4O_MINI,
        OpenRouterConfig.MISTRAL_LARGE,
        OpenRouterConfig.MISTRAL_MEDIUM,
        OpenRouterConfig.MISTRAL_SMALL,
        OpenRouterConfig.QWEN_14B,
        OpenRouterConfig.QWEN_7B,
        OpenRouterConfig.LLAMA_8B,
        OpenRouterConfig.GEMMA
    )

    override suspend fun analyzeCV(cvContent: String): Result<CvAnalysis> = runCatching {
        val apiKey = apiKeyStore.getUserKey().ifBlank { buildKey }
        if (apiKey.isBlank()) {
            LlmLogger.logError("API ключ не настроен")
            throw IllegalStateException("API ключ не настроен")
        }

        // Адаптивные настройки токенов
        val tokenLevels = listOf(2000, 1000, 300, 200, 100, 50)

        // Язык ответа модели — под язык интерфейса приложения
        val respondInEnglish =
            AppCompatDelegate.getApplicationLocales()[0]?.language == "en"
        val systemPrompt = LlmConstants.systemPrompt(respondInEnglish)

        // Пробуем разные модели в порядке приоритета
        var lastError: Exception? = null

        for (model in modelPriority) {
            for (maxTokens in tokenLevels) {
                try {
                    LlmLogger.logApiRequest(model, cvContent.length)
                    LlmLogger.logDebug("Пробуем модель $model с maxTokens=$maxTokens")

                    // Валидация входных данных
                    if (cvContent.isBlank()) {
                        throw IllegalArgumentException("Пустой контент резюме")
                    }

                    if (cvContent.length > 100000) {
                        LlmLogger.logWarning("Контент резюме слишком длинный: ${cvContent.length} символов")
                    }

                    val request = LlmRequestDto(
                        model = model,
                        messages = listOf(
                            MessageDto(
                                role = "system",
                                content = systemPrompt
                            ),
                            MessageDto(
                                role = "user",
                                content = cvContent
                            )
                        ),
                        maxTokens = maxTokens,
                        temperature = 0.3,
                        topP = 0.9,
                        frequencyPenalty = 0.1,
                        presencePenalty = 0.1,
                        // Для OpenAI моделей просим строгий JSON
                        responseFormat = if (model.startsWith("openai/")) ResponseFormatDto(type = "json_object") else null
                    )

                    // Логируем детали запроса для диагностики
                    LlmLogger.logDebug("Отправляем запрос к модели: $model")
                    // Не логируем ключ даже частично в проде
                    LlmLogger.logDebug("Длина системного промпта: ${systemPrompt.length}")
                    LlmLogger.logDebug("Длина контента пользователя: ${cvContent.length}")
                    LlmLogger.logDebug("maxTokens: $maxTokens")

                    val response = api.analyzeCv("Bearer $apiKey", request)

                    if (response.choices.isEmpty()) {
                        throw IllegalStateException("Пустой ответ от OpenRouter API для модели $model")
                    }

                    val content = response.choices.first().message.content
                    LlmLogger.logApiResponse(content.length)
                    LlmLogger.logAnalysisResult(
                        "Успешный анализ с моделью",
                        "$model (${maxTokens} токенов)"
                    )

                    // Оцениваем качество, но парсим исходный контент без "очистки",
                    // т.к. очистка может повредить JSON
                    val quality = LlmResponseProcessor.validateResponseQuality(content)

                    LlmLogger.logAnalysisResult(
                        "Качество ответа",
                        "Слов: ${quality.wordCount}, Повторения: ${(quality.repetitionRatio * 100).toInt()}%, Общих фраз: ${quality.genericPhraseCount}"
                    )

                    if (!quality.isGoodQuality) {
                        LlmLogger.logAnalysisResult(
                            "Предупреждение",
                            "Низкое качество ответа, попробуйте другую модель"
                        )
                    }

                    val analysis = LlmResponseProcessor.parseJson(content)

                    // Пустой разбор = модель вернула мусор: считаем попытку
                    // проваленной и уходим к следующей модели, а не отдаём
                    // пользователю «успешный» пустой анализ
                    if (analysis.isBlank()) {
                        throw IllegalStateException("Модель $model вернула нечитаемый JSON-ответ")
                    }

                    return@runCatching analysis

                } catch (e: HttpException) {
                    lastError = e
                    val errorMessage = when (e.code()) {
                        400 -> "Некорректный запрос к модели $model. Проверьте параметры запроса."
                        401 -> "Неверный API ключ для модели $model. Проверьте настройки."
                        402 -> "Недостаточно кредитов для модели $model с ${maxTokens} токенами. Пробуем меньше токенов."
                        403 -> "Доступ запрещен к модели $model. Проверьте права доступа."
                        429 -> "Превышен лимит запросов для модели $model. Попробуйте позже."
                        500 -> "Ошибка сервера для модели $model. Попробуйте другую модель."
                        else -> "HTTP ошибка ${e.code()} для модели $model: ${e.message()}"
                    }
                    LlmLogger.logError(errorMessage, e)

                    // Пауза между попытками: без backoff подряд идущие запросы
                    // на 429/лимитах только усугубляют троттлинг
                    delay(RETRY_DELAY_MS)

                    // Если это ошибка 402, продолжаем с меньшим количеством токенов
                    if (e.code() == 402) {
                        continue
                    } else {
                        break // Для других ошибок переходим к следующей модели
                    }
                } catch (e: Exception) {
                    lastError = e
                    LlmLogger.logError("Ошибка с моделью $model: ${e.message}", e)
                    delay(RETRY_DELAY_MS)
                    break // Переходим к следующей модели
                }
            }
        }

        // Если все модели не сработали
        val finalErrorMessage = when (lastError) {
            is HttpException -> {
                when (lastError.code()) {
                    400 -> "Все модели вернули ошибку 400. Проверьте корректность данных резюме."
                    401 -> "Неверный API ключ. Проверьте настройки OpenRouter API."
                    402 -> "Недостаточно кредитов даже с минимальным количеством токенов. Пополните баланс на OpenRouter."
                    403 -> "Доступ запрещен ко всем моделям. Проверьте права доступа."
                    429 -> "Превышен лимит запросов ко всем моделям. Попробуйте позже."
                    else -> "HTTP ошибка ${lastError.code()}: ${lastError.message()}"
                }
            }

            else -> "Не удалось проанализировать резюме: ${lastError?.message ?: "Неизвестная ошибка"}"
        }

        LlmLogger.logError(finalErrorMessage, lastError)
        throw lastError ?: IllegalStateException("Не удалось проанализировать резюме")

    }.onFailure { error ->
        LlmLogger.logError("Ошибка во время анализа резюме", error)
    }
} 