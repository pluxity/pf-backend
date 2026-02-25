package com.pluxity.yongin.goal.dto

import com.pluxity.yongin.goal.entity.ConstructionSection
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시공구간 응답")
data class ConstructionSectionResponse(
    @field:Schema(description = "시공구간 ID", example = "1")
    val id: Long,
    @field:Schema(description = "시공구간명", example = "절토")
    val name: String,
)

fun ConstructionSection.toResponse(): ConstructionSectionResponse =
    ConstructionSectionResponse(
        id = this.requiredId,
        name = this.name,
    )
