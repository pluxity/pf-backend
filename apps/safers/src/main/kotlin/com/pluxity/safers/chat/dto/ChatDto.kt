package com.pluxity.safers.chat.dto

data class ChatRequest(
    val message: String,
)

data class ChatResponse(
    val messages: List<A2uiMessage>,
)
