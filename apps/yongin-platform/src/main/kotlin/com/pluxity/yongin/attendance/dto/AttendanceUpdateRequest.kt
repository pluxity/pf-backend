package com.pluxity.yongin.attendance.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "출역현황 작업내용 수정 요청")
data class AttendanceUpdateRequest(
    @field:Schema(description = "금일작업내용", example = "콘크리트 타설 작업")
    @field:NotBlank
    val workContent: String,
)
