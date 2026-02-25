package com.pluxity.yongin.processstatus.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "공정명 등록 요청")
data class WorkTypeRequest(
    @field:Schema(description = "공정명", example = "토공", required = true)
    @field:NotBlank(message = "공정명은 필수입니다")
    val name: String,
)
