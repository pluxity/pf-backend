package com.pluxity.safers.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ChatActionRequest(
    @field:NotBlank
    @field:Schema(description = "조회 액션 ID", example = "evt_1")
    val actionId: String,
    @field:Schema(description = "조회 대상", example = "EVENT")
    val target: QueryTarget,
    @field:Schema(description = "조회 필터")
    val filters: ActionFilter,
    @field:Min(1)
    @field:Schema(description = "페이지 번호 (1부터 시작)", example = "1")
    val page: Int = 1,
    @field:Min(1)
    @field:Schema(description = "페이지 크기", example = "50")
    val size: Int = 50,
)
