package com.pluxity.yongin.announcement.dto

fun dummyAnnouncementRequest(content: String = "테스트 안내사항") =
    AnnouncementRequest(
        content = content,
    )
