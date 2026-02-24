package com.pluxity.yonginplatform.processstatus.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공정현황 일괄 저장/수정/삭제 요청")
data class ProcessStatusBulkRequest(
    @field:Schema(description = "저장/수정할 공정현황 목록")
    val upserts: List<ProcessStatusRequest> = emptyList(),
    @field:Schema(description = "삭제할 공정현황 ID 목록", example = "[1, 2]")
    val deletedIds: List<Long> = emptyList(),
)
