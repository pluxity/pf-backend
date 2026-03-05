package com.pluxity.weekly.project.dto

import com.pluxity.weekly.project.entity.ProjectAssignment
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로젝트 배정 응답")
data class ProjectAssignmentResponse(
    @field:Schema(description = "배정 ID", example = "1")
    val id: Long,
    @field:Schema(description = "프로젝트 ID", example = "1")
    val projectId: Long,
    @field:Schema(description = "배정된 사용자 ID", example = "1")
    val userId: Long,
)

fun ProjectAssignment.toResponse(): ProjectAssignmentResponse =
    ProjectAssignmentResponse(
        id = this.requiredId,
        projectId = this.project.requiredId,
        userId = this.assignedBy.requiredId,
    )
