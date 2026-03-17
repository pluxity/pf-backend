package com.pluxity.weekly.chat.service

import com.pluxity.weekly.chat.dto.ChatReadResponse
import com.pluxity.weekly.chat.dto.LlmAction
import com.pluxity.weekly.chat.dto.TaskSearchFilter
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.project.service.ProjectService
import com.pluxity.weekly.task.entity.TaskStatus
import com.pluxity.weekly.task.service.TaskService
import com.pluxity.weekly.team.service.TeamService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ChatReadHandler(
    private val taskService: TaskService,
    private val projectService: ProjectService,
    private val epicService: EpicService,
    private val teamService: TeamService,
) {
    fun handle(action: LlmAction): ChatReadResponse {
        val target = action.target ?: "task"
        val filters = action.filters ?: emptyMap()

        return when (target) {
            "task" ->
                ChatReadResponse(
                    tasks = taskService.search(buildTaskFilter(filters)),
                )
            "project" ->
                ChatReadResponse(
                    projects = projectService.findAll(),
                )
            "epic" ->
                ChatReadResponse(
                    epics = epicService.findAll(),
                )
            "team" ->
                ChatReadResponse(
                    teams = teamService.findAll(),
                )
            else ->
                ChatReadResponse(
                    tasks = taskService.search(buildTaskFilter(filters)),
                )
        }
    }

    private fun buildTaskFilter(filters: Map<String, Any?>): TaskSearchFilter =
        TaskSearchFilter(
            status = (filters["status"] as? String)?.let { TaskStatus.valueOf(it) },
            epicId = (filters["epic_id"] as? Number)?.toLong(),
            projectId = (filters["project_id"] as? Number)?.toLong(),
            assigneeId = (filters["assignee_id"] as? Number)?.toLong(),
            name = filters["name"] as? String,
            dueDateFrom = (filters["due_date_from"] as? String)?.let { LocalDate.parse(it) },
            dueDateTo = (filters["due_date_to"] as? String)?.let { LocalDate.parse(it) },
        )
}
