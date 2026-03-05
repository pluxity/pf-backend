package com.pluxity.weekly.epic.service

import com.pluxity.common.core.exception.CustomException
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

@Service
@Transactional(readOnly = true)
class EpicService(
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
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

    private fun getEpicById(id: Long): Epic =
        epicRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC, id)

    private fun getProjectById(id: Long): Project =
        projectRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, id)

    private fun getTeamById(id: Long): Team =
        teamRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, id)
}
