package com.pluxity.weekly.team.dto

import com.pluxity.weekly.team.entity.TeamMember
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팀원 응답")
data class TeamMemberResponse(
    @field:Schema(description = "팀원 ID", example = "1")
    val id: Long,
    @field:Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "사용자명", example = "홍길동")
    val userName: String,
)

fun TeamMember.toResponse(): TeamMemberResponse =
    TeamMemberResponse(
        id = this.requiredId,
        teamId = this.team.requiredId,
        userId = this.user.requiredId,
        userName = this.user.name,
    )
