package com.pluxity.safers.cctv.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "CCTV 재생 요청")
data class CctvPlaybackRequest(
    @field:NotBlank
    @field:Schema(description = "시작 일시 (yyyyMMddHHmmss)", example = "20260304130000")
    val startDate: String,
    @field:NotBlank
    @field:Schema(description = "종료 일시 (yyyyMMddHHmmss)", example = "20260304140000")
    val endDate: String,
)
