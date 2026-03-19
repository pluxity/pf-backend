package com.pluxity.weekly.chat.context

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.project.dto.ProjectResponse
import com.pluxity.weekly.project.service.ProjectService
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.task.service.TaskService
import com.pluxity.weekly.team.service.TeamService
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

/**
 * target + action 별 CONTEXT 포함 데이터
 *
 * project create/update → projects(simple) + users(PM 권한)
 * project delete        → projects(simple)
 * epic    create/update → projects > epics + users(전체)
 * epic    delete        → projects > epics
 * task    create        → projects > epics
 * task    update/delete → projects > epics > tasks
 * team    create/update → teams + users(전체)
 * team    delete        → teams
 *
 * TODO: 권한별 조회 — findAll() + 필터 대신 사용자 권한 기반 조회 메서드로 교체
 */
@Component
@Transactional(readOnly = true)
class ContextBuilder(
    private val userRepository: UserRepository,
    private val projectService: ProjectService,
    private val epicService: EpicService,
    private val taskService: TaskService,
    private val teamService: TeamService,
    private val authorizationService: AuthorizationService,
    private val objectMapper: ObjectMapper,
) {
    fun build(
        target: String,
        actions: List<String>,
    ): String {
        val user = authorizationService.currentUser()

        authorizationService.checkChatPermission(user, target, actions)

        val context =
            mutableMapOf<String, Any?>(
                "today" to LocalDate.now().toString(),
                "user" to mapOf("id" to user.requiredId, "name" to user.name),
            )

        val hasMutation = "create" in actions || "update" in actions
        val hasCreateOnly = "create" in actions && "update" !in actions

        when (target) {
            "project" -> buildProjectContext(context, hasMutation)
            "epic" -> buildEpicContext(context, hasMutation)
            "team" -> buildTeamContext(context, hasMutation)
            else -> buildTaskContext(context, hasCreateOnly)
        }

        return objectMapper.writeValueAsString(context)
    }

    private fun buildProjectContext(
        context: MutableMap<String, Any?>,
        hasMutation: Boolean,
    ) {
        val projects = projectService.findAll()
        context["projects"] = projects.map { projectMapSimple(it) }
        if (hasMutation) {
            context["users"] = findUsersByRole("PM")
        }
    }

    private fun buildEpicContext(
        context: MutableMap<String, Any?>,
        hasMutation: Boolean,
    ) {
        val projects = projectService.findAll()
        val epics = epicService.findAll()
        val epicsByProject = epics.groupBy { it.projectId }
        context["projects"] =
            projects.map { project ->
                projectMapWithEpics(project, epicsByProject[project.id] ?: emptyList())
            }
        if (hasMutation) {
            context["users"] = findAllUsers()
        }
    }

    private fun buildTaskContext(
        context: MutableMap<String, Any?>,
        createOnly: Boolean,
    ) {
        val projects = projectService.findAll()
        val epics = epicService.findAll()
        val epicsByProject = epics.groupBy { it.projectId }

        if (createOnly) {
            // create: 에픽까지만 (태스크 불필요)
            context["projects"] =
                projects.map { project ->
                    projectMapWithEpics(project, epicsByProject[project.id] ?: emptyList())
                }
        } else {
            // update/delete/read: full hierarchy
            val tasks = taskService.findAll()
            val tasksByEpicId = tasks.groupBy { it.epicId }
            context["projects"] =
                projects.map { project ->
                    projectMapFull(project, epicsByProject[project.id] ?: emptyList(), tasksByEpicId)
                }
        }
    }

    private fun buildTeamContext(
        context: MutableMap<String, Any?>,
        hasMutation: Boolean,
    ) {
        context["teams"] = teamService.findAll().map { mapOf("id" to it.id, "name" to it.name) }
        if (hasMutation) {
            context["users"] = findAllUsers()
        }
    }

    private fun findUsersByRole(roleName: String): List<Map<String, Any?>> =
        userRepository
            .findAllBy(Sort.by("name"))
            .filter { user -> user.userRoles.any { it.role.name.uppercase() == roleName } }
            .map { mapOf("id" to it.requiredId, "name" to it.name) }

    private fun findAllUsers(): List<Map<String, Any?>> =
        userRepository
            .findAllBy(Sort.by("name"))
            .map { mapOf("id" to it.requiredId, "name" to it.name) }

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
