package com.pluxity.yongin.goal.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "시공구간 등록 요청")
data class ConstructionSectionRequest(
    @field:Schema(description = "시공구간명", example = "절토", required = true)
    @field:NotBlank(message = "시공구간명은 필수입니다")
    val name: String,
)
