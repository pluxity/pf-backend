package com.pluxity.common.auth.permission.dto

import com.pluxity.common.auth.permission.ResourceTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "권한 설정 가능 리소스 타입 정보")
data class ResourceTypeResponse(
    @field:Schema(
        description = "리소스 타입의 고유 키 (Enum 상수 이름)",
        example = "FACILITY",
    ) val key: String,
    @field:Schema(
        description = "리소스 타입의 한글 이름",
        example = "시설",
    ) val name: String,
    @field:Schema(
        description = "관련 API 엔드포인트 경로",
        example = "facilities",
    ) val endpoint: String,
    @field:Schema(
        description = "해당 타입의 리소스 목록",
    ) val resources: List<ResourceItemResponse> = emptyList(),
)

fun ResourceTypeInfo.toResponse(resources: List<ResourceItemResponse> = emptyList()): ResourceTypeResponse =
    ResourceTypeResponse(
        key = key,
        name = resourceName,
        endpoint = endpoint,
        resources = resources,
    )
