package com.pluxity.weekly.project.service

import com.pluxity.common.auth.annotation.CheckPermission
import com.pluxity.common.auth.user.entity.PermissionAction
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.dto.ProjectAssignmentResponse
import com.pluxity.weekly.project.dto.ProjectRequest
import com.pluxity.weekly.project.dto.ProjectResponse
import com.pluxity.weekly.project.dto.toResponse
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.repository.ProjectRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO: ProjectStatus 상태 전이 규칙 (예: TODO → IN_PROGRESS → DONE, 역방향 제한 등)
// TODO: dueDate 초과 시 status를 OVERDUE로 변경하는 스케줄러 또는 조회 시 판정 로직

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    @CheckPermission(action = PermissionAction.READ_LIST, resourceType = "project")
    fun findAll(): List<ProjectResponse> {
        val projects = projectRepository.findAll()
        if (projects.isEmpty()) return emptyList()
        val memberMap =
            projectRepository
                .findMembersByProjectIds(projects.map { it.requiredId })
                .groupBy { it.projectId }
        return projects.map { it.toResponse(memberMap[it.requiredId].orEmpty()) }
    }

    @CheckPermission(action = PermissionAction.READ_SINGLE, resourceType = "project")
    fun findById(id: Long): ProjectResponse {
        val project = getById(id)
        return project.toResponse(projectRepository.findMembersByProjectId(project.requiredId))
    }

    @CheckPermission(action = PermissionAction.CREATE, resourceType = "project")
    @Transactional
    fun create(request: ProjectRequest): Long =
        projectRepository
            .save(
                Project(
                    name = request.name,
                    description = request.description,
                    status = request.status,
                    startDate = request.startDate,
                    dueDate = request.dueDate,
                    pmId = request.pmId,
                ),
            ).requiredId

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "project")
    @Transactional
    fun update(
        id: Long,
        request: ProjectRequest,
    ) {
        getById(id).update(
            name = request.name,
            description = request.description,
            status = request.status,
            startDate = request.startDate,
            dueDate = request.dueDate,
            pmId = request.pmId,
        )
    }

    @CheckPermission(action = PermissionAction.DELETE, resourceType = "project")
    @Transactional
    fun delete(id: Long) {
        projectRepository.delete(getById(id))
    }

    // ── ProjectAssignment ──

    @CheckPermission(action = PermissionAction.READ_LIST, resourceType = "project")
    fun findAssignments(projectId: Long): List<ProjectAssignmentResponse> = getById(projectId).assignments.map { it.toResponse() }

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "project")
    @Transactional
    fun assign(
        projectId: Long,
        userId: Long,
    ) {
        val project = getById(projectId)
        val user = getUserById(userId)
        if (project.assignments.any { it.assignedBy == user }) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_PROJECT_ASSIGNMENT, userId, projectId)
        }
        project.assign(user)
    }

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "project")
    @Transactional
    fun unassign(
        projectId: Long,
        userId: Long,
    ) {
        val project = getById(projectId)
        val user = getUserById(userId)
        if (project.assignments.none { it.assignedBy == user }) {
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT_ASSIGNMENT, projectId, userId)
        }
        project.unassign(user)
    }

    private fun getById(id: Long): Project =
        projectRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, id)

    private fun getUserById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, id)
}
