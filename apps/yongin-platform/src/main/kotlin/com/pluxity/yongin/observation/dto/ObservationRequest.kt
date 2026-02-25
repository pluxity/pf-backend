package com.pluxity.yongin.observation.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@Schema(description = "드론 관측 데이터 요청")
data class ObservationRequest(
    @field:Schema(description = "날짜", example = "2026-01-27", required = true)
    val date: LocalDate,
    @field:Schema(description = "설명", example = "드론 관측 데이터")
    val description: String?,
    @field:Schema(description = "파일 ID", example = "1", required = true)
    val fileId: Long,
    @field:Schema(description = "루트 파일명", example = "tileset.json", required = true)
    @field:NotBlank(message = "루트 파일명은 필수입니다")
    val rootFileName: String,
)
