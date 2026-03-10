package com.pluxity.weekly.chat.llm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    @JsonProperty("keep_alive")
    val keepAlive: String = "30m",
    val think: Boolean = false,
    val options: OllamaOptions = OllamaOptions(),
)

data class OllamaMessage(
    val role: String,
    val content: String,
)

data class OllamaOptions(
    val temperature: Double = 0.1,
)
