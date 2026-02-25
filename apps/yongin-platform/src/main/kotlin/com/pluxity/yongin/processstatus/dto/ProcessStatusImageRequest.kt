package com.pluxity.yongin.processstatus.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공정관련 이미지 수정 요청")
data class ProcessStatusImageRequest(
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long,
)
