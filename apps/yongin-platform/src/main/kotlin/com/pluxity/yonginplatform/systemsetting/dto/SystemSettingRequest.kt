package com.pluxity.yonginplatform.systemsetting.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "시스템 설정 요청")
data class SystemSettingRequest(
    @field:Min(value = 1, message = "롤링 간격은 1초 이상이어야 합니다.")
    @field:Schema(description = "롤링 간격(초)", example = "10")
    val rollingIntervalSeconds: Int,
    @field:Schema(description = "BIM 썸네일 파일 ID", example = "1")
    val bimThumbnailFileId: Long? = null,
    @field:Schema(description = "조감도 파일 ID", example = "2")
    val aerialViewFileId: Long? = null,
)
