package com.pluxity.weekly.epic.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.entity.EpicStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "에픽 응답")
data class EpicResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "프로젝트 ID", example = "1")
    val projectId: Long,
    @field:Schema(description = "에픽명", example = "사용자 인증 모듈")
    val name: String,
    @field:Schema(description = "설명", example = "에픽 설명입니다")
    val description: String?,
    @field:Schema(description = "상태", example = "TODO")
    val status: EpicStatus,
    @field:Schema(description = "시작일", example = "2026-01-01")
    val startDate: LocalDate?,
    @field:Schema(description = "마감일", example = "2026-03-31")
    val dueDate: LocalDate?,
    @field:Schema(description = "담당 팀 ID", example = "1")
    val teamId: Long?,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Epic.toResponse(): EpicResponse =
    EpicResponse(
        id = this.requiredId,
        projectId = this.project.requiredId,
        name = this.name,
        description = this.description,
        status = this.status,
        startDate = this.startDate,
        dueDate = this.dueDate,
        teamId = this.team?.requiredId,
        baseResponse = this.toBaseResponse(),
    )
