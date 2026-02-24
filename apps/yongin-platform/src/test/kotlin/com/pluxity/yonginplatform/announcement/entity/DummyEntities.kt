package com.pluxity.yonginplatform.announcement.entity

import com.pluxity.common.core.test.withAudit

fun dummyAnnouncement(
    id: Long = Announcement.SINGLETON_ID,
    content: String = "테스트 안내사항",
) = Announcement(
    id = id,
    content = content,
).withAudit()
