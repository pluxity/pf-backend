package com.pluxity.yongin.notice.dto

import java.time.LocalDate

fun dummyNoticeRequest(
    title: String = "테스트 공지사항",
    content: String = "테스트 공지사항 내용입니다",
    isVisible: Boolean = false,
    isAlways: Boolean = false,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
) = NoticeRequest(
    title = title,
    content = content,
    isVisible = isVisible,
    isAlways = isAlways,
    startDate = startDate,
    endDate = endDate,
)
