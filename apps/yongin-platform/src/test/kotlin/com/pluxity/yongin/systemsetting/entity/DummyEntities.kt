package com.pluxity.yongin.systemsetting.entity

import com.pluxity.common.core.test.withAudit

fun dummySystemSetting(
    rollingIntervalSeconds: Int = 10,
    bimThumbnailFileId: Long? = null,
    aerialViewFileId: Long? = null,
) = SystemSetting(
    rollingIntervalSeconds = rollingIntervalSeconds,
    bimThumbnailFileId = bimThumbnailFileId,
    aerialViewFileId = aerialViewFileId,
).withAudit()
