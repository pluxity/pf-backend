package com.pluxity.safers.cctv.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CCTV 재생 응답")
data class CctvPlaybackResponse(
    @field:Schema(description = "재생 경로명", example = "playback-pb_38ae550d")
    val pathName: String,
)
