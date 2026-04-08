package com.pluxity.safers.chat.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.safers.chat.dto.ChatRequest
import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.service.ChatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat Controller", description = "자연어 채팅 API (A2UI)")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping
    @Operation(summary = "자연어 질문으로 데이터 조회 및 A2UI 응답 생성")
    fun chat(
        principal: Principal,
        @Valid @RequestBody request: ChatRequest,
    ): ResponseEntity<DataResponseBody<ChatResponse>> =
        ResponseEntity.ok(DataResponseBody(chatService.chat(principal.name, request.message)))
}
