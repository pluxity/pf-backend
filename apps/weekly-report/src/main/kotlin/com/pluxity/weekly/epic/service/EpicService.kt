package com.pluxity.weekly.epic.service

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.weekly.chat.dto.EpicSearchFilter
import com.pluxity.weekly.epic.dto.EpicAssignmentResponse
import com.pluxity.weekly.epic.dto.EpicRequest
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.epic.dto.EpicUpdateRequest
import com.pluxity.weekly.epic.dto.toResponse
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.global.constant.UserType
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.repository.ProjectRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO: EpicStatus 상태 전이 규칙 (예: TODO → IN_PROGRESS → DONE, 역방향 제한 등)
// TODO: Role별 llm context 생성 메서드
@Service
@Transactional(readOnly = true)
class EpicService(
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val authorizationService: AuthorizationService,
) {
    fun findAll(): List<EpicResponse> {
        val user = authorizationService.currentUser()
        if (user.isAdmin()) return epicRepository.findAll().map { it.toResponse() }
        if (user.userRoles.any { it.role.name.equals(UserType.PM.roleName, ignoreCase = true) }) {
            return epicRepository
                .findByProjectIdIn(
                    projectRepository.findByPmId(user.requiredId).map { it.requiredId },
                ).map { it.toResponse() }
        }
        return epicRepository.findByAssignmentsUserId(user.requiredId).map { it.toResponse() }
    }

    fun search(filter: EpicSearchFilter): List<EpicResponse> {
        if (filter.assigneeId != null) {
            return epicRepository.findByAssignmentsUserId(filter.assigneeId).map { it.toResponse() }
        }
        return epicRepository
            .findAllNotNull {
                select(entity(Epic::class))
                    .from(entity(Epic::class))
                    .whereAnd(
                        filter.status?.let { path(Epic::status).eq(it) },
                        filter.name?.let { path(Epic::name).like("%$it%") },
                        filter.projectId?.let { path(Epic::project)(Project::id).eq(it) },
                    )
            }.map { it.toResponse() }
    }

    fun findById(id: Long): EpicResponse = getEpicById(id).toResponse()

    @Transactional
    fun create(request: EpicRequest): Long {
        val user = authorizationService.currentUser()
        authorizationService.requireEpicManage(user, request.projectId)
        val epic =
            epicRepository.save(
                Epic(
                    project = getProjectById(request.projectId),
                    name = request.name,
                    description = request.description,
                    status = request.status,
                    startDate = request.startDate,
                    dueDate = request.dueDate,
                ),
            )
        request.userIds?.forEach { userId ->
            val assignee = getUserById(userId)
            epic.assign(assignee)
        }
        return epic.requiredId
    }

    @Transactional
    fun update(
        id: Long,
        request: EpicUpdateRequest,
    ) {
        val user = authorizationService.currentUser()
        val epic = getEpicById(id)
        authorizationService.requireEpicManage(user, epic.project.requiredId)
        epic.update(
            project = request.projectId?.let { getProjectById(it) },
            name = request.name,
            description = request.description,
            status = request.status,
            startDate = request.startDate,
            dueDate = request.dueDate,
        )
        request.userIds?.forEach { userId ->
            val assignee = getUserById(userId)
            if (epic.assignments.none { it.user == assignee }) {
                epic.assign(assignee)
            }
        }
    }

    @Transactional
    fun delete(id: Long) {
        val user = authorizationService.currentUser()
        val epic = getEpicById(id)
        authorizationService.requireEpicManage(user, epic.project.requiredId)
        epicRepository.delete(epic)
    }

    // ── EpicAssignment ──

    fun findAssignments(epicId: Long): List<EpicAssignmentResponse> = getEpicById(epicId).assignments.map { it.toResponse() }

    @Transactional
    fun assign(
        epicId: Long,
        userId: Long,
    ) {
        val user = authorizationService.currentUser()
        authorizationService.requireEpicAssign(user, epicId)
        val epic = getEpicById(epicId)
        val assignee = getUserById(userId)
        if (epic.assignments.any { it.user == assignee }) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_EPIC_ASSIGNMENT, userId, epicId)
        }
        epic.assign(assignee)
    }

    @Transactional
    fun unassign(
        epicId: Long,
        userId: Long,
    ) {
        val user = authorizationService.currentUser()
        authorizationService.requireEpicAssign(user, epicId)
        val epic = getEpicById(epicId)
        val assignee = getUserById(userId)
        if (epic.assignments.none { it.user == assignee }) {
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC_ASSIGNMENT, epicId, userId)
        }
        epic.unassign(assignee)
    }

    private fun getEpicById(id: Long): Epic =
        epicRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC, id)

    private fun getProjectById(id: Long): Project =
        projectRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, id)

    private fun getUserById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, id)
}
