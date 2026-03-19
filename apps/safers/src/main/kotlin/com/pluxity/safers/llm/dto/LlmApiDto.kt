package com.pluxity.safers.llm.dto

// OpenRouter (OpenAI 호환)
data class Message(
    val role: String,
    val content: String,
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
)

data class ChatCompletionResponse(
    val choices: List<Choice>? = null,
)

data class Choice(
    val message: ChoiceMessage? = null,
)

data class ChoiceMessage(
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
    val message: ChoiceMessage? = null,
)
