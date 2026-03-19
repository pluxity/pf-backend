package com.pluxity.weekly.project.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.global.constant.UserType
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.dto.ProjectRequest
import com.pluxity.weekly.project.dto.ProjectResponse
import com.pluxity.weekly.project.dto.ProjectUpdateRequest
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
    private val authorizationService: AuthorizationService,
) {
    fun findAll(): List<ProjectResponse> {
        val user = authorizationService.currentUser()
        val projects =
            if (user.isAdmin()) {
                projectRepository.findAll()
            } else if (user.userRoles.any { it.role.name.equals(UserType.PM.roleName, ignoreCase = true) }) {
                projectRepository.findByPmId(user.requiredId)
            } else {
                emptyList()
            }
        if (projects.isEmpty()) return emptyList()
        val memberMap =
            projectRepository
                .findMembersByProjectIds(projects.map { it.requiredId })
                .groupBy { it.projectId }
        return projects.map { it.toResponse(memberMap[it.requiredId].orEmpty()) }
    }

    fun findById(id: Long): ProjectResponse {
        val project = getById(id)
        return project.toResponse(projectRepository.findMembersByProjectId(project.requiredId))
    }

    @Transactional
    fun create(request: ProjectRequest): Long {
        val user = authorizationService.currentUser()
        authorizationService.requireAdmin(user)
        return projectRepository
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
    }

    @Transactional
    fun update(
        id: Long,
        request: ProjectUpdateRequest,
    ) {
        val user = authorizationService.currentUser()
        authorizationService.requireProjectManager(user, id)
        getById(id).update(
            name = request.name,
            description = request.description,
            status = request.status,
            startDate = request.startDate,
            dueDate = request.dueDate,
            pmId = request.pmId,
        )
    }

    @Transactional
    fun delete(id: Long) {
        val user = authorizationService.currentUser()
        authorizationService.requireProjectManager(user, id)
        projectRepository.delete(getById(id))
    }

    private fun getById(id: Long): Project =
        projectRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, id)
}
