package com.pluxity.weekly.epic.service

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.epic.dto.EpicAssignmentResponse
import com.pluxity.weekly.epic.dto.EpicRequest
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.epic.dto.toResponse
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.team.entity.Team
import com.pluxity.weekly.team.repository.TeamRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO: @CheckPermission 적용 (Epic 도메인 권한 체크)
// TODO: EpicStatus 상태 전이 규칙 (예: TODO → IN_PROGRESS → DONE, 역방향 제한 등)
// TODO: ProjectAssignment, EpicAssignment 통합
// TODO: Role별 llm context 생성 메서드
@Service
@Transactional(readOnly = true)
class EpicService(
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(): List<EpicResponse> = epicRepository.findAll().map { it.toResponse() }

    fun findById(id: Long): EpicResponse = getEpicById(id).toResponse()

    @Transactional
    fun create(request: EpicRequest): Long =
        epicRepository
            .save(
                Epic(
                    project = getProjectById(request.projectId),
                    name = request.name,
                    description = request.description,
                    status = request.status,
                    startDate = request.startDate,
                    dueDate = request.dueDate,
                    team = request.teamId?.let { getTeamById(it) },
                ),
            ).requiredId

    @Transactional
    fun update(
        id: Long,
        request: EpicRequest,
    ) {
        getEpicById(id).update(
            project = getProjectById(request.projectId),
            name = request.name,
            description = request.description,
            status = request.status,
            startDate = request.startDate,
            dueDate = request.dueDate,
            team = request.teamId?.let { getTeamById(it) },
        )
    }

    @Transactional
    fun delete(id: Long) {
        epicRepository.delete(getEpicById(id))
    }

    // ── EpicAssignment ──

    fun findAssignments(epicId: Long): List<EpicAssignmentResponse> = getEpicById(epicId).assignments.map { it.toResponse() }

    @Transactional
    fun assign(
        epicId: Long,
        userId: Long,
    ) {
        val epic = getEpicById(epicId)
        val user = getUserById(userId)
        if (epic.assignments.any { it.assignedBy == user }) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_EPIC_ASSIGNMENT, userId, epicId)
        }
        epic.assign(user)
    }

    @Transactional
    fun unassign(
        epicId: Long,
        userId: Long,
    ) {
        val epic = getEpicById(epicId)
        val user = getUserById(userId)
        if (epic.assignments.none { it.assignedBy == user }) {
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC_ASSIGNMENT, epicId, userId)
        }
        epic.unassign(user)
    }

    private fun getEpicById(id: Long): Epic =
        epicRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC, id)

    private fun getProjectById(id: Long): Project =
        projectRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, id)

    private fun getTeamById(id: Long): Team =
        teamRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, id)

    private fun getUserById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, id)
}
