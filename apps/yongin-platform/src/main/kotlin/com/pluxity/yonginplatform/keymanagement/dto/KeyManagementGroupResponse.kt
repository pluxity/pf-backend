package com.pluxity.yonginplatform.keymanagement.dto

import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주요관리사항 타입별 그룹 응답")
data class KeyManagementGroupResponse(
    @field:Schema(description = "타입", example = "QUALITY")
    val type: KeyManagementType,
    @field:Schema(description = "타입 설명", example = "품질")
    val typeDescription: String,
    @field:Schema(description = "해당 타입의 주요관리사항 목록")
    val items: List<KeyManagementResponse>,
)
