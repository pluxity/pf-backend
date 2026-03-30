package com.pluxity.weekly.chat.context

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.global.auth.AuthorizationService
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
 * 에픽 기준 바텀업으로 프로젝트 hierarchy 구성 (권한 필터링된 에픽에서 프로젝트 역그루핑)
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
        context["projects"] =
            projects.map {
                mapOf("id" to it.id, "name" to it.name, "status" to it.status.name)
            }
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
                mapOf(
                    "id" to project.id,
                    "name" to project.name,
                    "epics" to
                        (epicsByProject[project.id] ?: emptyList()).map {
                            mapOf("id" to it.id, "name" to it.name)
                        },
                )
            }
        if (hasMutation) {
            context["users"] = findAllUsers()
        }
    }

    private fun buildTaskContext(
        context: MutableMap<String, Any?>,
        createOnly: Boolean,
    ) {
        val epics = epicService.findAll()

        if (createOnly) {
            context["projects"] = groupByProject(epics)
        } else {
            val tasks = taskService.findAll()
            val tasksByEpicId = tasks.groupBy { it.epicId }
            context["projects"] = groupByProjectFull(epics, tasksByEpicId)
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

    private fun groupByProject(epics: List<EpicResponse>): List<Map<String, Any?>> =
        epics
            .groupBy { it.projectId to it.projectName }
            .map { (key, epics) ->
                mapOf(
                    "id" to key.first,
                    "name" to key.second,
                    "epics" to epics.map { mapOf("id" to it.id, "name" to it.name) },
                )
            }

    private fun groupByProjectFull(
        epics: List<EpicResponse>,
        tasksByEpicId: Map<Long, List<TaskResponse>>,
    ): List<Map<String, Any?>> =
        epics
            .groupBy { it.projectId to it.projectName }
            .map { (key, epics) ->
                mapOf(
                    "id" to key.first,
                    "name" to key.second,
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

    private fun findUsersByRole(roleName: String): List<Map<String, Any?>> =
        userRepository
            .findAllBy(Sort.by("name"))
            .filter { user -> user.userRoles.any { it.role.name.uppercase() == roleName } }
            .map { mapOf("id" to it.requiredId, "name" to it.name) }

    private fun findAllUsers(): List<Map<String, Any?>> =
        userRepository
            .findAllBy(Sort.by("name"))
            .map { mapOf("id" to it.requiredId, "name" to it.name) }
}
