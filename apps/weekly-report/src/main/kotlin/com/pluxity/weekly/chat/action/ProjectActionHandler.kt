package com.pluxity.weekly.chat.action

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.weekly.chat.action.dto.ActionRequest
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.FieldSpec
import com.pluxity.weekly.chat.action.dto.ResolveAction
import com.pluxity.weekly.project.dto.ProjectRequest
import com.pluxity.weekly.project.entity.ProjectStatus
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.project.service.ProjectService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ProjectActionHandler(
    private val projectService: ProjectService,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    fun handle(
        action: ActionRequest,
        userId: Long,
        actionType: ActionType,
    ): ActionResult =
        when (actionType) {
            ActionType.READ -> handleRead(action, actionType)
            ActionType.CREATE -> handleCreate(action, actionType)
            ActionType.UPDATE -> handleUpdate(action, actionType)
            ActionType.DELETE -> handleDelete(action, actionType)
            else -> ActionResult(ActionResultType.ERROR, actionType, "프로젝트에 대해 지원하지 않는 액션입니다.", target = "project")
        }

    private fun handleRead(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val projects = projectService.findAll()
        val filters = action.filters ?: emptyMap()
        val filtered =
            projects.filter { p ->
                val statusMatch = (filters["status"] as? String)?.let { p.status.name == it.uppercase() } ?: true
                val nameMatch = (filters["name"] as? String)?.let { p.name.contains(it, ignoreCase = true) } ?: true
                statusMatch && nameMatch
            }
        val results =
            filtered.map { p ->
                mapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "status" to p.status.name,
                    "startDate" to p.startDate,
                    "dueDate" to p.dueDate,
                )
            }
        return ActionResult(ActionResultType.SUCCESS, actionType, "${filtered.size}개의 프로젝트를 조회했습니다.", results, target = "project")
    }

    private fun handleCreate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val resolve = action as? ResolveAction
        val pmId = resolve?.pmId

        // resolve에서 모든 필드가 채워진 경우 → 생성
        if (action.name != null && pmId != null) {
            val status = action.status?.let { parseEnum<ProjectStatus>(it) } ?: ProjectStatus.TODO
            val request =
                ProjectRequest(
                    name = action.name!!,
                    description = action.description?.takeIf { it.isNotBlank() },
                    status = status,
                    startDate = action.startDate?.let { LocalDate.parse(it) },
                    dueDate = action.dueDate?.let { LocalDate.parse(it) },
                    pmId = pmId,
                )
            val id = projectService.create(request)
            return ActionResult(ActionResultType.SUCCESS, actionType, "프로젝트 '${action.name}'이(가) 생성되었습니다. (ID: $id)", target = "project")
        }

        // 필드 목록 한번에 내려주기
        val users = userRepository.findAll()
        val pmOptions =
            users.map { u ->
                mapOf("value" to u.requiredId.toString(), "label" to u.name)
            }
        return ActionResult(
            ActionResultType.CLARIFY,
            actionType,
            "프로젝트를 생성합니다. 아래 정보를 입력해주세요.",
            target = "project",
            partial =
                buildMap {
                    put("action", "create")
                    put("target", "project")
                    if (action.name != null) put("name", action.name)
                },
            requiredFields =
                listOf(
                    FieldSpec(key = "name", label = "프로젝트명", required = true),
                    FieldSpec(key = "description", label = "설명"),
                    FieldSpec(key = "pmId", label = "PM", type = "select", required = true, options = pmOptions),
                    FieldSpec(key = "startDate", label = "시작일", type = "date"),
                    FieldSpec(key = "dueDate", label = "마감일", type = "date"),
                ),
        )
    }

    private fun handleUpdate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val projectId =
            action.id ?: resolveProjectId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "수정할 프로젝트를 찾을 수 없습니다.", target = "project")

        val status = action.status?.let { parseEnum<ProjectStatus>(it) } ?: ProjectStatus.TODO
        val request =
            ProjectRequest(
                name = action.name ?: return ActionResult(ActionResultType.ERROR, actionType, "프로젝트 이름이 필요합니다.", target = "project"),
                description = action.description,
                status = status,
                startDate = action.startDate?.let { LocalDate.parse(it) },
                dueDate = action.dueDate?.let { LocalDate.parse(it) },
            )
        projectService.update(projectId, request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "프로젝트 '${request.name}'이(가) 수정되었습니다.", target = "project")
    }

    private fun handleDelete(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val id =
            action.id ?: resolveProjectId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "삭제할 프로젝트를 찾을 수 없습니다.", target = "project")
        return ActionResult(
            type = ActionResultType.NEEDS_CONFIRM,
            action = actionType,
            message = "프로젝트(ID: $id)을(를) 삭제하시겠습니까?",
            data = mapOf("projectId" to id),
            target = "project",
        )
    }

    fun resolveProjectId(action: ActionRequest): Long? {
        if (action.projectId != null) return action.projectId
        val name = action.project ?: return null
        val matches = projectRepository.findByNameContainingIgnoreCase(name)
        return matches.firstOrNull()?.requiredId
    }
}
