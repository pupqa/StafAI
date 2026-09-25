package com.bober.autcsv.core.di

import com.bober.autcsv.BuildConfig
import com.bober.autcsv.core.constants.LlmConstants
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.data.api.llm.OpenRouterApi
import com.bober.autcsv.data.api.llm.OpenRouterService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * DI-модуль сети: OkHttp, перехватчики, Retrofit, API и сервис OpenRouter.
 */
object NetworkModule {

    @Provides
    @Singleton
            /**
             * Перехватчик для логирования и категоризации HTTP-ошибок.
             */
    fun provideErrorInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                // peekBody оставляет тело нетронутым для downstream-обработчиков
                // (HttpException.errorBody()); обрезаем, чтобы не льёт в лог
                val errorBody = runCatching {
                    response.peekBody(1L shl 20).string().take(400)
                }.getOrNull() ?: "Unknown error"
                LlmLogger.logError("HTTP ${response.code} error: $errorBody")

                when (response.code) {
                    400 -> {
                        LlmLogger.logError("Bad Request (400): Проверьте корректность запроса к OpenRouter API")
                    }

                    401 -> {
                        LlmLogger.logError("Unauthorized (401): Проверьте API ключ OpenRouter")
                    }

                    402 -> {
                        LlmLogger.logError("Payment Required (402): Недостаточно кредитов. Уменьшите max_tokens или пополните баланс на https://openrouter.ai/settings/credits")
                    }

                    403 -> {
                        LlmLogger.logError("Forbidden (403): Доступ запрещен к OpenRouter API")
                    }

                    429 -> {
                        LlmLogger.logError("Too Many Requests (429): Превышен лимит запросов к OpenRouter API")
                    }

                    500 -> {
                        LlmLogger.logError("Internal Server Error (500): Ошибка сервера OpenRouter")
                    }

                    else -> {
                        LlmLogger.logError("HTTP ${response.code}: Неожиданная ошибка от OpenRouter API")
                    }
                }
            }

            response
        }
    }

    @Provides
    @Singleton
            /**
             * Клиент OkHttp с таймаутами, ретраями и логированием.
             */
    fun provideOkHttpClient(errorInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(errorInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
            /**
             * Retrofit API-клиент OpenRouter.
             */
    fun provideOpenRouterApi(okHttpClient: OkHttpClient): OpenRouterApi {
        return Retrofit.Builder()
            .baseUrl(LlmConstants.OPENROUTER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }

    @Provides
    @Named("openrouter_api_key")
            /**
             * API-ключ OpenRouter из BuildConfig.
             */
    fun provideOpenRouterApiKey(): String {
        return BuildConfig.OPENROUTER_API_KEY
    }

    @Provides
    @Singleton
            /**
             * Сервис-обертка над OpenRouter API: ключ читается лениво,
             * сохранённый пользователем приоритетнее ключа сборки.
             */
    fun provideOpenRouterService(
        api: OpenRouterApi,
        apiKeyStore: com.bober.autcsv.core.utils.ApiKeyStore,
        @Named("openrouter_api_key") apiKey: String,
    ): OpenRouterService {
        return OpenRouterService(api, apiKeyStore, apiKey)
    }
}