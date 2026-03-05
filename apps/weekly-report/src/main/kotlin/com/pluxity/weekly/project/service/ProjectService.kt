package com.pluxity.weekly.project.service

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

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(): List<ProjectResponse> =
        projectRepository.findAll().map {
            it.toResponse(projectRepository.findMembersByProjectId(it.requiredId))
        }

    fun findById(id: Long): ProjectResponse {
        val project = getById(id)
        return project.toResponse(projectRepository.findMembersByProjectId(project.requiredId))
    }

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

    @Transactional
    fun delete(id: Long) {
        projectRepository.deleteById(getById(id).requiredId)
    }

    // ── ProjectAssignment ──

    fun findAssignments(projectId: Long): List<ProjectAssignmentResponse> = getById(projectId).assignments.map { it.toResponse() }

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
