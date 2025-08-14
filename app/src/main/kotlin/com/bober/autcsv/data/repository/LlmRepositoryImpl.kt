package com.bober.autcsv.data.repository

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
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named
import kotlin.Result

class LlmRepositoryImpl @Inject constructor(
    private val api: OpenRouterApi,
    @Named("openrouter_api_key") private val apiKey: String,
) : LlmRepository {

    // Список моделей в порядке приоритета (от лучшей к худшей)
    private val modelPriority = listOf(
        OpenRouterConfig.CLAUDE_3_5_SONNET,
        OpenRouterConfig.CLAUDE_3_5_HAIKU,
        OpenRouterConfig.GPT_4O,
        OpenRouterConfig.GPT_4O_MINI,
        OpenRouterConfig.MISTRAL_LARGE,
        OpenRouterConfig.MISTRAL_MEDIUM,
        OpenRouterConfig.MISTRAL_SMALL
    )

    override suspend fun analyzeCV(cvContent: String): Result<CvAnalysis> = runCatching {
        if (apiKey.isBlank()) {
            LlmLogger.logError("API ключ не настроен")
            throw IllegalStateException("API ключ не настроен")
        }

        // Адаптивные настройки токенов
        val tokenLevels = listOf(2000, 1000, 300, 200, 100, 50)

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
                                content = LlmConstants.SYSTEM_PROMPT
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
                    LlmLogger.logDebug("Длина системного промпта: ${LlmConstants.SYSTEM_PROMPT.length}")
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

                    return@runCatching LlmResponseProcessor.parseJson(content)

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

                    // Если это ошибка 402, продолжаем с меньшим количеством токенов
                    if (e.code() == 402) {
                        continue
                    } else {
                        break // Для других ошибок переходим к следующей модели
                    }
                } catch (e: Exception) {
                    lastError = e
                    LlmLogger.logError("Ошибка с моделью $model: ${e.message}", e)
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