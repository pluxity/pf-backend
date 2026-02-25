package com.pluxity.yongin.goal.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "목표관리 일괄 저장/수정/삭제 요청")
data class GoalBulkRequest(
    @field:Schema(description = "저장/수정할 목표관리 목록")
    val upserts: List<GoalRequest> = emptyList(),
    @field:Schema(description = "삭제할 목표관리 ID 목록", example = "[1, 2]")
    val deletedIds: List<Long> = emptyList(),
)
