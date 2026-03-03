package com.pluxity.yongin.cctv.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CCTV 수정 요청")
data class CctvUpdateRequest(
    @field:Schema(description = "이름", example = "1번 카메라")
    val name: String,
    @field:Schema(description = "경도", example = "127.0")
    val lon: Double?,
    @field:Schema(description = "위도", example = "37.0")
    val lat: Double?,
    @field:Schema(description = "고도", example = "50.0")
    val alt: Double?,
)
