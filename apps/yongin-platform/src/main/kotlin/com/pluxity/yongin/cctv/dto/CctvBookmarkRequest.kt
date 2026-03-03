package com.pluxity.yongin.cctv.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "CCTV 즐겨찾기 등록 요청")
data class CctvBookmarkRequest(
    @field:NotBlank(message = "CCTV 스트림명은 필수입니다")
    @field:Schema(description = "CCTV 스트림명", example = "CCTV-001")
    val streamName: String,
)

@Schema(description = "CCTV 즐겨찾기 순서 변경 요청")
data class CctvBookmarkOrderRequest(
    @field:Schema(description = "순서대로 정렬된 즐겨찾기 ID 목록", example = "[1, 3, 2, 4]")
    val ids: List<Long>,
)
