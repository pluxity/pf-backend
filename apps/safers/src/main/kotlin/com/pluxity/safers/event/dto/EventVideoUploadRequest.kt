package com.pluxity.safers.event.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "이벤트 영상 등록 요청")
data class EventVideoUploadRequest(
    @field:NotBlank
    @field:Schema(description = "영상 URL", example = "http://localhost:8080/videos/event_clip_001.mp4")
    val video: String,
)
