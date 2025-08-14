package com.bober.autcsv.data.api.llm.dto

import com.google.gson.annotations.SerializedName

data class LlmRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerializedName("presence_penalty")
    val presencePenalty: Double? = null,
    val stream: Boolean = false,
    @SerializedName("n")
    val n: Int = 1
)

data class MessageDto(
    val role: String,
    val content: String
)

data class LlmResponseDto(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

data class ChoiceDto(
    val index: Int? = null,
    val message: MessageDto,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class UsageDto(
    @SerializedName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerializedName("completion_tokens")
    val completionTokens: Int? = null,
    @SerializedName("total_tokens")
    val totalTokens: Int? = null
) 