package com.pluxity.yonginplatform.keymanagement.dto

import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주요관리사항 타입 응답")
data class KeyManagementTypeResponse(
    @field:Schema(description = "타입 코드", example = "QUALITY")
    val code: String,
    @field:Schema(description = "타입 설명", example = "품질")
    val description: String,
)

fun KeyManagementType.toResponse(): KeyManagementTypeResponse =
    KeyManagementTypeResponse(
        code = this.name,
        description = this.description,
    )
