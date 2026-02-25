package com.pluxity.yongin.processstatus.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "공정현황 등록/수정 요청")
data class ProcessStatusRequest(
    @field:Schema(description = "공정현황 ID (수정 시 필수, 등록 시 null)", example = "1")
    val id: Long?,
    @field:Schema(description = "작업일", example = "2026-01-15", required = true)
    val workDate: LocalDate,
    @field:Schema(description = "공정명 ID", example = "1", required = true)
    val workTypeId: Long,
    @field:Schema(description = "목표율", example = "80")
    val plannedRate: Int,
    @field:Schema(description = "공정률", example = "75")
    val actualRate: Int,
    @field:Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean = false,
)
