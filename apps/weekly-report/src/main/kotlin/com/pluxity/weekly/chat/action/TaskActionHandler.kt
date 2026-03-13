package com.pluxity.weekly.chat.action

import com.pluxity.weekly.chat.action.dto.ActionRequest
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.FieldSpec
import com.pluxity.weekly.chat.dto.TaskSearchFilter
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.dto.TaskRequest
import com.pluxity.weekly.task.entity.TaskStatus
import com.pluxity.weekly.task.repository.TaskRepository
import com.pluxity.weekly.task.service.TaskService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TaskActionHandler(
    private val taskService: TaskService,
    private val taskRepository: TaskRepository,
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
) {
    fun handle(
        action: ActionRequest,
        userId: Long,
        actionType: ActionType,
    ): ActionResult =
        when (actionType) {
            ActionType.READ -> handleRead(action, actionType)
            ActionType.CREATE -> handleCreate(action, userId, actionType)
            ActionType.UPDATE -> handleUpdate(action, actionType)
            ActionType.DELETE -> handleDelete(action, actionType)
            else -> ActionResult(ActionResultType.ERROR, actionType, "태스크에 대해 지원하지 않는 액션입니다.", target = "task")
        }

    private fun handleRead(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val filter = buildTaskFilter(action)
        val tasks = taskService.search(filter)
        val results =
            tasks.groupBy { it.projectId }.map { (_, projectTasks) ->
                val first = projectTasks.first()
                mapOf(
                    "project" to first.projectName,
                    "epics" to
                        projectTasks.groupBy { it.epicId }.map { (_, epicTasks) ->
                            val epicFirst = epicTasks.first()
                            mapOf(
                                "name" to epicFirst.epicName,
                                "tasks" to
                                    epicTasks.map { task ->
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

        return ActionResult(ActionResultType.SUCCESS, actionType, "${tasks.size}개의 태스크를 조회했습니다.", results, target = "task")
    }

    private fun handleCreate(
        action: ActionRequest,
        userId: Long,
        actionType: ActionType,
    ): ActionResult {
        val epicId = resolveEpicId(action)
        val name = action.name

        if (name == null || epicId == null) {
            return ActionResult(
                ActionResultType.CLARIFY,
                actionType,
                "태스크를 생성합니다. 아래 정보를 입력해주세요.",
                target = "task",
                partial =
                    buildMap {
                        put("action", "create")
                        put("target", "task")
                        if (name != null) put("name", name)
                        if (action.epic != null) put("epic", action.epic)
                        if (action.project != null) put("project", action.project)
                    },
                requiredFields =
                    listOf(
                        FieldSpec(key = "project", label = "프로젝트명", required = true),
                        FieldSpec(key = "epic", label = "에픽명", required = true),
                        FieldSpec(key = "name", label = "태스크명", required = true),
                        FieldSpec(key = "description", label = "설명"),
                        FieldSpec(key = "startDate", label = "시작일", type = "date"),
                        FieldSpec(key = "dueDate", label = "마감일", type = "date"),
                    ),
            )
        }

        val status = action.status?.let { parseEnum<TaskStatus>(it) } ?: TaskStatus.TODO
        val request =
            TaskRequest(
                epicId = epicId,
                name = name,
                description = action.description,
                status = status,
                progress = action.progress ?: if (status == TaskStatus.DONE) 100 else 0,
                startDate = action.startDate?.let { LocalDate.parse(it) },
                dueDate = action.dueDate?.let { LocalDate.parse(it) },
                assigneeId = userId,
            )

        val id = taskService.create(request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "태스크 '$name'이(가) 생성되었습니다. (ID: $id)", target = "task")
    }

    private fun handleUpdate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val taskId =
            action.id ?: resolveTaskId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "수정할 태스크를 찾을 수 없습니다.", target = "task")

        val existing =
            taskRepository.findByIdOrNull(taskId)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "태스크(ID: $taskId)를 찾을 수 없습니다.", target = "task")

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

        taskService.update(taskId, request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "태스크 '${request.name}'이(가) 수정되었습니다.", target = "task")
    }

    private fun handleDelete(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val id =
            action.id ?: resolveTaskId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "삭제할 태스크를 찾을 수 없습니다.", target = "task")
        return ActionResult(
            type = ActionResultType.NEEDS_CONFIRM,
            action = actionType,
            message = "태스크(ID: $id)을(를) 삭제하시겠습니까?",
            data = mapOf("taskId" to id),
            target = "task",
        )
    }

    private fun resolveTaskId(action: ActionRequest): Long? {
        val name = action.name ?: return null
        val epicId = resolveEpicId(action) ?: return null
        return taskRepository.findByEpicIdAndName(epicId, name)?.requiredId
    }

    fun resolveEpicId(action: ActionRequest): Long? {
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

    fun resolveProjectId(action: ActionRequest): Long? {
        if (action.projectId != null) return action.projectId
        val name = action.project ?: return null
        val matches = projectRepository.findByNameContainingIgnoreCase(name)
        return matches.firstOrNull()?.requiredId
    }

    private fun buildTaskFilter(action: ActionRequest): TaskSearchFilter {
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
}
