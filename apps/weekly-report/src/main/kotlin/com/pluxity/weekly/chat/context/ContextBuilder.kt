package com.pluxity.weekly.chat.context

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.dto.ProjectResponse
import com.pluxity.weekly.project.service.ProjectService
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.task.service.TaskService
import com.pluxity.weekly.team.service.TeamService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@Component
class ContextBuilder(
    private val userRepository: UserRepository,
    private val projectService: ProjectService,
    private val epicService: EpicService,
    private val taskService: TaskService,
    private val teamService: TeamService,
    private val objectMapper: ObjectMapper,
) {
    fun build(userId: Long): String = build(userId, "task")

    fun build(
        userId: Long,
        target: String,
    ): String {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, userId)

        val context =
            mutableMapOf<String, Any?>(
                "today" to LocalDate.now().toString(),
                "user" to mapOf("id" to user.requiredId, "name" to user.name),
            )

        when (target) {
            "project" -> {
                val projects = projectService.findAll()
                context["projects"] = projects.map { projectMapSimple(it) }
            }
            "epic" -> {
                val projects = projectService.findAll()
                val epics = epicService.findAll()
                val epicsByProject = epics.groupBy { it.projectId }
                context["projects"] =
                    projects.map { project ->
                        projectMapWithEpics(project, epicsByProject[project.id] ?: emptyList())
                    }
            }
            "team" -> {
                val page =
                    teamService.findAll(
                        com.pluxity.common.core.dto
                            .PageSearchRequest(page = 1, size = 100),
                    )
                context["teams"] = page.content.map { mapOf("id" to it.id, "name" to it.name) }
            }
            else -> {
                // task: full hierarchy
                val projects = projectService.findAll()
                val epics = epicService.findAll()
                val tasks = taskService.findAll()
                val tasksByEpicId = tasks.groupBy { it.epicId }
                val epicsByProject = epics.groupBy { it.projectId }
                context["projects"] =
                    projects.map { project ->
                        projectMapFull(project, epicsByProject[project.id] ?: emptyList(), tasksByEpicId)
                    }
            }
        }

        return objectMapper.writeValueAsString(context)
    }

    private fun projectMapSimple(project: ProjectResponse): Map<String, Any?> =
        mapOf(
            "id" to project.id,
            "name" to project.name,
            "status" to project.status.name,
        )

    private fun projectMapWithEpics(
        project: ProjectResponse,
        epics: List<EpicResponse>,
    ): Map<String, Any?> =
        mapOf(
            "id" to project.id,
            "name" to project.name,
            "epics" to
                    epics.map { epic ->
                        mapOf("id" to epic.id, "name" to epic.name)
                    },
        )

    private fun projectMapFull(
        project: ProjectResponse,
        epics: List<EpicResponse>,
        tasksByEpicId: Map<Long, List<TaskResponse>>,
    ): Map<String, Any?> =
        mapOf(
            "id" to project.id,
            "name" to project.name,
            "epics" to
                    epics.map { epic ->
                        mapOf(
                            "id" to epic.id,
                            "name" to epic.name,
                            "tasks" to
                                    (tasksByEpicId[epic.id] ?: emptyList()).map { task ->
                                        mapOf(
                                            "id" to task.id,
                                            "name" to task.name,
                                            "status" to task.status,
                                            "progress" to task.progress,
                                        )
                                    },
                        )
                    },
        )
}
