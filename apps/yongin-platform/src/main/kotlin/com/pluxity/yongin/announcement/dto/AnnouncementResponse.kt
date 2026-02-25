package com.pluxity.yongin.announcement.dto

import com.pluxity.yongin.announcement.entity.Announcement
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "안내사항 응답")
data class AnnouncementResponse(
    @field:Schema(description = "안내사항 내용", example = "오늘의 안내사항입니다.")
    val content: String? = null,
    @field:Schema(description = "수정일시")
    val updatedAt: LocalDateTime? = null,
)

fun Announcement.toResponse(): AnnouncementResponse =
    AnnouncementResponse(
        content = this.content,
        updatedAt = this.updatedAt,
    )
