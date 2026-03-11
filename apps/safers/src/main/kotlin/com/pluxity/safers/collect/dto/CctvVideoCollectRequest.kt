package com.pluxity.safers.collect.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "CCTV 영상 수집 요청")
data class CctvVideoCollectRequest(
    @field:NotBlank
    @field:Schema(description = "이벤트 ID", example = "EVT-20260101-001")
    @field:JsonProperty("event_id")
    val eventId: String,
    @field:NotBlank
    @field:Schema(description = "영상 URL", example = "http://localhost:8080/videos/event_clip_001.mp4")
    val video: String,
)
