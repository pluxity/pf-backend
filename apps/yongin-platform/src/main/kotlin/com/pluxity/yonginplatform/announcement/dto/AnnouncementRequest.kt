package com.pluxity.yonginplatform.announcement.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "안내사항 수정 요청")
data class AnnouncementRequest(
    @field:Schema(description = "안내사항 내용", example = "오늘의 안내사항입니다.")
    @field:NotBlank(message = "안내사항 내용은 공백이 될 수 없습니다.")
    val content: String,
)
