package com.pluxity.yongin.systemsetting.dto

fun dummySystemSettingRequest(
    rollingIntervalSeconds: Int = 10,
    bimThumbnailFileId: Long? = null,
    aerialViewFileId: Long? = null,
) = SystemSettingRequest(
    rollingIntervalSeconds = rollingIntervalSeconds,
    bimThumbnailFileId = bimThumbnailFileId,
    aerialViewFileId = aerialViewFileId,
)

fun dummySystemSettingResponse(rollingIntervalSeconds: Int = 30) =
    SystemSettingResponse(
        rollingIntervalSeconds = rollingIntervalSeconds,
    )
