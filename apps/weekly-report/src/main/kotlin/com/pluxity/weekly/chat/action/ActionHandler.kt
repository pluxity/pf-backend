package com.pluxity.weekly.chat.action

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.LlmAction
import com.pluxity.weekly.chat.dto.TaskSearchFilter
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.dto.TaskRequest
import com.pluxity.weekly.task.entity.TaskStatus
import com.pluxity.weekly.task.repository.TaskRepository
import com.pluxity.weekly.task.service.TaskService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Component
class ActionHandler(
    private val taskService: TaskService,
    private val taskRepository: TaskRepository,
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
) {
    fun handle(
        action: LlmAction,
        userId: Long,
    ): ActionResult {
        val actionType = resolveActionType(action.action)
        return try {
            when (actionType) {
                ActionType.READ -> handleRead(action, actionType)
                ActionType.UPSERT -> handleUpsert(action, userId, actionType)
                ActionType.DELETE -> handleDelete(action, actionType)
                ActionType.CLARIFY -> handleClarify(action, actionType)
                ActionType.UNKNOWN -> ActionResult(ActionResultType.ERROR, actionType, "알 수 없는 액션: ${action.action}")
            }
        } catch (e: CustomException) {
            log.warn { "액션 처리 중 권한/비즈니스 오류: ${e.message}" }
            ActionResult(ActionResultType.ERROR, actionType, e.message ?: "처리 중 오류가 발생했습니다.")
        }
    }

    private fun resolveActionType(action: String): ActionType =
        when (action) {
            "read" -> ActionType.READ
            "upsert" -> ActionType.UPSERT
            "delete" -> ActionType.DELETE
            "clarify" -> ActionType.CLARIFY
            else -> ActionType.UNKNOWN
        }

    private fun handleRead(
        action: LlmAction,
        actionType: ActionType,
    ): ActionResult {
        val filter = buildTaskFilter(action)
        val tasks = taskService.search(filter)
        val results = tasks.groupBy { it.projectId }.map { (_, projectTasks) ->
            val first = projectTasks.first()
            mapOf(
                "project" to first.projectName,
                "epics" to projectTasks.groupBy { it.epicId }.map { (_, epicTasks) ->
                    val epicFirst = epicTasks.first()
                    mapOf(
                        "name" to epicFirst.epicName,
                        "tasks" to epicTasks.map { task ->
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

        return ActionResult(ActionResultType.SUCCESS, actionType, "${tasks.size}개의 태스크를 조회했습니다.", results)
    }

    private fun handleUpsert(
        action: LlmAction,
        userId: Long,
        actionType: ActionType,
    ): ActionResult {
        val taskId = action.id ?: resolveTaskId(action)
        if (taskId != null) {
            return updateTask(action.copy(id = taskId), actionType)
        }
        return createTask(action, userId, actionType)
    }

    private fun createTask(
        action: LlmAction,
        userId: Long,
        actionType: ActionType,
    ): ActionResult {
        val epicId =
            resolveEpicId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "에픽을 찾을 수 없습니다.")

        val status = action.status?.let { parseEnum<TaskStatus>(it) } ?: TaskStatus.TODO
        val request =
            TaskRequest(
                epicId = epicId,
                name = action.name ?: return ActionResult(ActionResultType.ERROR, actionType, "태스크 이름이 필요합니다."),
                description = action.description,
                status = status,
                progress = action.progress ?: if (status == TaskStatus.DONE) 100 else 0,
                startDate = action.startDate?.let { LocalDate.parse(it) },
                dueDate = action.dueDate?.let { LocalDate.parse(it) },
                assigneeId = userId,
            )

        val id = taskService.create(request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "태스크 '${action.name}'이(가) 생성되었습니다. (ID: $id)")
    }

    private fun updateTask(
        action: LlmAction,
        actionType: ActionType,
    ): ActionResult {
        val taskId =
            action.id
                ?: return ActionResult(ActionResultType.ERROR, actionType, "태스크 ID가 null 입니다.")

        val existing =
            taskRepository.findByIdOrNull(taskId)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "태스크(ID: ${action.id})를 찾을 수 없습니다.")

        val epicId = resolveEpicId(action) ?: existing.epic.requiredId
        val status = action.status?.let { parseEnum<TaskStatus>(it) } ?: existing.status
        val request =
            TaskRequest(
                epicId = epicId,
                name = action.name ?: existing.name,
                description = action.description ?: existing.description,
                status = status,
                progress = action.progress ?: existing.progress,
                startDate = action.startDate?.let { LocalDate.parse(it) } ?: existing.startDate,
                dueDate = action.dueDate?.let { LocalDate.parse(it) } ?: existing.dueDate,
                assigneeId = existing.assignee?.requiredId,
            )

        taskService.update(action.id, request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "태스크 '${request.name}'이(가) 수정되었습니다.")
    }

    private fun handleDelete(
        action: LlmAction,
        actionType: ActionType,
    ): ActionResult {
        val id = action.id ?: resolveTaskId(action)
            ?: return ActionResult(ActionResultType.ERROR, actionType, "삭제할 태스크를 찾을 수 없습니다.")
        return ActionResult(
            type = ActionResultType.NEEDS_CONFIRM,
            action = actionType,
            message = "태스크(ID: $id)을(를) 삭제하시겠습니까?",
            data = mapOf("taskId" to id),
        )
    }

    private fun handleClarify(
        action: LlmAction,
        actionType: ActionType,
    ): ActionResult =
        ActionResult(
            type = ActionResultType.CLARIFY,
            action = actionType,
            message = action.message ?: "추가 정보가 필요합니다.",
            candidates = action.candidates,
            partial = action.partial,
        )

    private fun resolveTaskId(action: LlmAction): Long? {
        val name = action.name ?: return null
        val epicId = resolveEpicId(action) ?: return null
        return taskRepository.findByEpicIdAndName(epicId, name)?.requiredId
    }

    private fun resolveProjectId(action: LlmAction): Long? {
        if (action.projectId != null) return action.projectId
        val name = action.project ?: return null
        val matches = projectRepository.findByNameContainingIgnoreCase(name)
        return matches.firstOrNull()?.requiredId
    }

    private fun resolveEpicId(action: LlmAction): Long? {
        if (action.epicId != null) return action.epicId
        val name = action.epic ?: return null
        val projectId = resolveProjectId(action)
        val matches =
            if (projectId != null) {
                epicRepository.findByNameContainingIgnoreCaseAndProjectId(name, projectId)
            } else {
                epicRepository.findByNameContainingIgnoreCase(name)
            }
        return matches.firstOrNull()?.requiredId
    }

    private fun buildTaskFilter(action: LlmAction): TaskSearchFilter {
        val filters = action.filters ?: emptyMap()
        val projectId = resolveProjectId(action)
        val epicId = resolveEpicId(action)

        return TaskSearchFilter(
            status = (filters["status"] as? String)?.let { parseEnum<TaskStatus>(it) },
            epicId = epicId,
            projectId = projectId,
            assigneeId = (filters["assignee_id"] as? Number)?.toLong(),
            name = filters["name"] as? String,
            dueDateFrom = (filters["due_date_from"] as? String)?.let { LocalDate.parse(it) },
            dueDateTo = (filters["due_date_to"] as? String)?.let { LocalDate.parse(it) },
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String): T? =
        try {
            enumValueOf<T>(value.uppercase().replace(" ", "_"))
        } catch (_: IllegalArgumentException) {
            null
        }
}
