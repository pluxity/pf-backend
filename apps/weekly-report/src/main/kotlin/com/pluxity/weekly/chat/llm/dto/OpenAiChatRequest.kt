package com.pluxity.weekly.chat.llm.dto

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.1,
)

data class OpenAiMessage(
    val role: String,
    val content: String,
)
