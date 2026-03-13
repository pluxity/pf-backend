package com.pluxity.weekly.chat.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaChatResponse(
    val model: String? = null,
    val message: OllamaMessage? = null,
    val done: Boolean = false,
    @param:JsonProperty("prompt_eval_count")
    val promptEvalCount: Int = 0,
    @param:JsonProperty("eval_count")
    val evalCount: Int = 0,
    @param:JsonProperty("prompt_eval_duration")
    val promptEvalDuration: Long = 0,
    @param:JsonProperty("eval_duration")
    val evalDuration: Long = 0,
    @param:JsonProperty("total_duration")
    val totalDuration: Long = 0,
    @param:JsonProperty("load_duration")
    val loadDuration: Long = 0,
)
