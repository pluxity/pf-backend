package com.pluxity.yonginplatform.cctv.dto

import com.pluxity.yonginplatform.cctv.entity.CctvFavorite
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "CCTV 즐겨찾기 응답")
data class CctvFavoriteResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "CCTV 스트림명", example = "CCTV-001")
    val streamName: String,
    @field:Schema(description = "표시 순서", example = "1")
    val displayOrder: Int,
    @field:Schema(description = "생성일시")
    val createdAt: LocalDateTime,
    @field:Schema(description = "생성자")
    val createdBy: String?,
)

fun CctvFavorite.toResponse(): CctvFavoriteResponse =
    CctvFavoriteResponse(
        id = this.requiredId,
        streamName = this.streamName,
        displayOrder = this.displayOrder,
        createdAt = this.createdAt,
        createdBy = this.createdBy,
    )
