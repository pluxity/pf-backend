package com.pluxity.yongin.processstatus.dto

import com.pluxity.yongin.processstatus.entity.WorkType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공정명 응답")
data class WorkTypeResponse(
    @field:Schema(description = "공정명 ID", example = "1")
    val id: Long,
    @field:Schema(description = "공정명", example = "토공")
    val name: String,
)

fun WorkType.toResponse(): WorkTypeResponse =
    WorkTypeResponse(
        id = this.requiredId,
        name = this.name,
    )
