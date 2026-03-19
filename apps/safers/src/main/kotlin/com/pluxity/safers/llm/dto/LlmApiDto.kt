package com.pluxity.safers.llm.dto

data class Message(
    val role: String,
    val content: String,
)

data class ResponseMessage(
    val content: String? = null,
    val role: String? = null,
)

// Ollama
data class OllamaChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean,
    val options: OllamaOptions,
)

data class OllamaOptions(
    val temperature: Double,
)

data class OllamaChatResponse(
    val message: ResponseMessage? = null,
)

// OpenRouter (OpenAI 호환)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
)

data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice>? = null,
)

data class OpenRouterChoice(
    val message: ResponseMessage? = null,
)
