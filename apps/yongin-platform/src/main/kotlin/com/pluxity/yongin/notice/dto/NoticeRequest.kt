package com.pluxity.yongin.notice.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "공지사항 등록/수정 요청")
data class NoticeRequest(
    @field:Schema(description = "제목 (최대 255자)", example = "공지사항 제목입니다", required = true, maxLength = 255)
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 최대 255자까지 입력 가능합니다")
    val title: String,
    @field:Schema(description = "내용 (최대 1000자)", example = "공지사항 내용입니다", required = true, maxLength = 1000)
    @field:NotBlank(message = "내용은 필수입니다")
    @field:Size(max = 1000, message = "내용은 최대 1000자까지 입력 가능합니다")
    val content: String,
    @field:Schema(description = "노출 여부", example = "false")
    val isVisible: Boolean = false,
    @field:Schema(description = "상시 게시 여부", example = "false")
    val isAlways: Boolean = false,
    @field:Schema(description = "게시 시작일", example = "2026-01-01")
    val startDate: LocalDate? = null,
    @field:Schema(description = "게시 종료일", example = "2026-12-31")
    val endDate: LocalDate? = null,
)
