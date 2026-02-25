package com.pluxity.yongin.notice.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import java.time.LocalDate

fun dummyNotice(
    id: Long? = null,
    title: String = "테스트 공지사항",
    content: String? = "테스트 공지사항 내용입니다",
    isVisible: Boolean = false,
    isAlways: Boolean = false,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
) = Notice(
    title = title,
    content = content,
    isVisible = isVisible,
    isAlways = isAlways,
    startDate = startDate,
    endDate = endDate,
).withId(id).withAudit()
