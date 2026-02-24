package com.pluxity.yonginplatform.systemsetting.dto

fun dummySystemSettingRequest(
    rollingIntervalSeconds: Int = 10,
    bimThumbnailFileId: Long? = null,
    aerialViewFileId: Long? = null,
) = SystemSettingRequest(
    rollingIntervalSeconds = rollingIntervalSeconds,
    bimThumbnailFileId = bimThumbnailFileId,
    aerialViewFileId = aerialViewFileId,
)
