package com.pluxity.weekly.chat.action

import com.pluxity.weekly.chat.action.dto.ActionRequest
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.FieldSpec
import com.pluxity.weekly.chat.action.dto.ResolveAction
import com.pluxity.weekly.epic.dto.EpicRequest
import com.pluxity.weekly.epic.entity.EpicStatus
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.project.service.ProjectService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EpicActionHandler(
    private val epicService: EpicService,
    private val epicRepository: EpicRepository,
    private val projectRepository: ProjectRepository,
    private val projectService: ProjectService,
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
            else -> ActionResult(ActionResultType.ERROR, actionType, "에픽에 대해 지원하지 않는 액션입니다.", target = "epic")
        }

    private fun handleRead(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {

        // todo: jdsl로 동적쿼리로 변경
        val epics = epicService.findAll()
        val filters = action.filters ?: emptyMap()
        val filtered =
            epics.filter { e ->
                val statusMatch = (filters["status"] as? String)?.let { e.status.name == it.uppercase() } ?: true
                val nameMatch = (filters["name"] as? String)?.let { e.name.contains(it, ignoreCase = true) } ?: true
                val projectMatch =
                    (filters["project"] as? String)?.let { pName ->
                        val projectId = projectRepository.findByNameContainingIgnoreCase(pName).firstOrNull()?.requiredId
                        projectId != null && e.projectId == projectId
                    } ?: true
                statusMatch && nameMatch && projectMatch
            }
        val results =
            // todo: response dto로 변경
            filtered.map { e ->
                mapOf(
                    "id" to e.id,
                    "name" to e.name,
                    "projectId" to e.projectId,
                    "status" to e.status.name,
                )
            }
        return ActionResult(ActionResultType.SUCCESS, actionType, "${filtered.size}개의 에픽을 조회했습니다.", results, target = "epic")
    }

    private fun handleCreate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val projectId = resolveProjectId(action)

        // resolve에서 모든 필수 필드가 채워진 경우 → 생성
        // 사실상 필요없음.
        if (projectId != null && action.name != null) {
            val status = action.status?.let { parseEnum<EpicStatus>(it) } ?: EpicStatus.TODO
            val request =
                EpicRequest(
                    projectId = projectId,
                    name = action.name!!,
                    description = action.description?.takeIf { it.isNotBlank() },
                    status = status,
                    startDate = action.startDate?.let { LocalDate.parse(it) },
                    dueDate = action.dueDate?.let { LocalDate.parse(it) },
                )
            val id = epicService.create(request)
            return ActionResult(ActionResultType.SUCCESS, actionType, "에픽 '${action.name}'이(가) 생성되었습니다. (ID: $id)", target = "epic")
        }

        // 필드 목록 한번에 내려주기
        // 프로젝트 조회도 권한대로
        val projects = projectService.findAll()
        val projectOptions =
            projects.map { p ->
                mapOf("value" to p.id.toString(), "label" to p.name)
            }
        return ActionResult(
            ActionResultType.CLARIFY,
            actionType,
            "에픽을 생성합니다. 아래 정보를 입력해주세요.",
            target = "epic",
            partial =
                buildMap {
                    put("action", "create")
                    put("target", "epic")
                    if (action.project != null) put("project", action.project)
                    if (action.projectId != null) put("projectId", action.projectId)
                    if (action.name != null) put("name", action.name)
                },
            requiredFields =
                listOf(
                    FieldSpec(key = "projectId", label = "프로젝트", type = "select", required = true, options = projectOptions),
                    FieldSpec(key = "name", label = "에픽명", required = true),
                    FieldSpec(key = "description", label = "설명"),
                    FieldSpec(key = "startDate", label = "시작일", type = "date"),
                    FieldSpec(key = "dueDate", label = "마감일", type = "date"),
                ),
        )
    }

    private fun handleUpdate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val epicId =
            action.id ?: action.epicId ?: resolveEpicId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "수정할 에픽을 찾을 수 없습니다.", target = "epic")

        val existing =
            epicRepository.findByIdOrNull(epicId)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "에픽(ID: $epicId)을 찾을 수 없습니다.", target = "epic")
        val status = action.status?.let { parseEnum<EpicStatus>(it) } ?: existing.status
        val name = action.name ?: existing.name
        val request =
            EpicRequest(
                projectId = existing.project.requiredId,
                name = name,
                description = action.description ?: existing.description,
                status = status,
                startDate = action.startDate?.let { LocalDate.parse(it) } ?: existing.startDate,
                dueDate = action.dueDate?.let { LocalDate.parse(it) } ?: existing.dueDate,
            )
        epicService.update(epicId, request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "에픽 '$name'이(가) 수정되었습니다.", target = "epic")
    }

    private fun handleDelete(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val id =
            action.id ?: resolveEpicId(action)
                ?: return ActionResult(ActionResultType.ERROR, actionType, "삭제할 에픽을 찾을 수 없습니다.", target = "epic")
        return ActionResult(
            type = ActionResultType.NEEDS_CONFIRM,
            action = actionType,
            message = "에픽(ID: $id)을(를) 삭제하시겠습니까?",
            data = mapOf("epicId" to id),
            target = "epic",
        )
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
}
