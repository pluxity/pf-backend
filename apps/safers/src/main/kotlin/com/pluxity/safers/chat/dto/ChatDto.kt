package com.pluxity.safers.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ChatRequest(
    @field:NotBlank
    @field:Schema(description = "사용자 자연어 질문", example = "서울 현장 CCTV 보여줘")
    val message: String,
)

data class ChatResponse(
    val messages: List<A2uiMessage>,
)
