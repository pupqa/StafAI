package com.bober.autcsv.data.api.llm

import com.bober.autcsv.data.api.llm.dto.LlmRequestDto
import com.bober.autcsv.data.api.llm.dto.LlmResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun analyzeCv(
        @Header("Authorization") authorization: String,
        @Body request: LlmRequestDto,
    ): LlmResponseDto
} 