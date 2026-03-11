package com.pluxity.weekly.chat.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null,
    val model: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChoice(
    val message: OpenAiMessage? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiUsage(
    @param:JsonProperty("prompt_tokens")
    val promptTokens: Int = 0,
    @param:JsonProperty("completion_tokens")
    val completionTokens: Int = 0,
    @param:JsonProperty("total_tokens")
    val totalTokens: Int = 0,
)
