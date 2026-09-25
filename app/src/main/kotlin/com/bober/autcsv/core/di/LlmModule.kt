package com.bober.autcsv.core.di

import com.bober.autcsv.data.api.llm.OpenRouterApi
import com.bober.autcsv.data.repository.LlmRepositoryImpl
import com.bober.autcsv.domain.repository.LlmRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * DI-модуль для провайда репозитория LLM.
 */
object LlmModule {
    @Provides
    @Singleton

    fun provideLlmRepository(
        api: OpenRouterApi,
        apiKeyStore: com.bober.autcsv.core.utils.ApiKeyStore,
        @Named("openrouter_api_key") apiKey: String,
    ): LlmRepository = LlmRepositoryImpl(api, apiKeyStore, apiKey)
}
