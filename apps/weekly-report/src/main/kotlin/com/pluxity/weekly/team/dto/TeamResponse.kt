package com.pluxity.weekly.team.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.weekly.team.entity.Team
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팀 응답")
data class TeamResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "팀명", example = "개발팀")
    val name: String,
    @field:Schema(description = "팀장 사용자 ID", example = "1")
    val leaderId: Long?,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Team.toResponse(): TeamResponse =
    TeamResponse(
        id = this.requiredId,
        name = this.name,
        leaderId = this.leaderId,
        baseResponse = this.toBaseResponse(),
    )
