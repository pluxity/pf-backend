package com.pluxity.yongin.announcement.dto

import java.time.LocalDateTime

fun dummyAnnouncementRequest(content: String = "테스트 안내사항") =
    AnnouncementRequest(
        content = content,
    )

fun dummyAnnouncementResponse(
    content: String? = "테스트 안내사항",
    updatedAt: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0),
) = AnnouncementResponse(
    content = content,
    updatedAt = updatedAt,
)
