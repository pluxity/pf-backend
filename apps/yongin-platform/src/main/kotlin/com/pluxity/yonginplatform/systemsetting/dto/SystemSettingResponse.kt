package com.pluxity.yonginplatform.systemsetting.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.yonginplatform.systemsetting.entity.SystemSetting
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시스템 설정 응답")
data class SystemSettingResponse(
    @field:Schema(description = "롤링 간격(초)")
    val rollingIntervalSeconds: Int? = null,
    @field:Schema(description = "BIM 썸네일 파일 정보")
    val bimThumbnailFile: FileResponse = FileResponse(),
    @field:Schema(description = "조감도 파일 정보")
    val aerialViewFile: FileResponse = FileResponse(),
)

fun SystemSetting.toResponse(
    bimThumbnailFile: FileResponse? = null,
    aerialViewFile: FileResponse? = null,
): SystemSettingResponse =
    SystemSettingResponse(
        rollingIntervalSeconds = rollingIntervalSeconds,
        bimThumbnailFile = bimThumbnailFile ?: FileResponse(),
        aerialViewFile = aerialViewFile ?: FileResponse(),
    )
