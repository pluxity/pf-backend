package com.pluxity.weekly.chat.action

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.weekly.chat.action.dto.ActionRequest
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.FieldSpec
import com.pluxity.weekly.team.dto.TeamRequest
import com.pluxity.weekly.team.service.TeamService
import org.springframework.stereotype.Component

@Component
class TeamActionHandler(
    private val teamService: TeamService,
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
            else -> ActionResult(ActionResultType.ERROR, actionType, "팀에 대해 지원하지 않는 액션입니다.", target = "team")
        }

    private fun handleRead(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val page = teamService.findAll(PageSearchRequest(page = 1, size = 100))
        val teams = page.content
        val filters = action.filters ?: emptyMap()
        val filtered =
            teams.filter { t ->
                val nameMatch = (filters["name"] as? String)?.let { t.name.contains(it, ignoreCase = true) } ?: true
                nameMatch
            }
        val results =
            filtered.map { t ->
                mapOf(
                    "id" to t.id,
                    "name" to t.name,
                    "leaderId" to t.leaderId,
                )
            }
        return ActionResult(ActionResultType.SUCCESS, actionType, "${filtered.size}개의 팀을 조회했습니다.", results, target = "team")
    }

    private fun handleCreate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val name =
            action.name
                ?: return ActionResult(
                    ActionResultType.CLARIFY,
                    actionType,
                    "팀을 생성합니다. 아래 정보를 입력해주세요.",
                    target = "team",
                    partial = mapOf("action" to "create", "target" to "team"),
                    requiredFields =
                        listOf(
                            FieldSpec(key = "name", label = "팀 이름", required = true),
                        ),
                )

        val request = TeamRequest(name = name)
        val id = teamService.create(request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "팀 '$name'이(가) 생성되었습니다. (ID: $id)", target = "team")
    }

    private fun handleUpdate(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val id =
            action.id
                ?: return ActionResult(ActionResultType.ERROR, actionType, "수정할 팀 ID가 필요합니다.", target = "team")
        val name =
            action.name
                ?: return ActionResult(ActionResultType.ERROR, actionType, "팀 이름이 필요합니다.", target = "team")
        val request = TeamRequest(name = name)
        teamService.update(id, request)
        return ActionResult(ActionResultType.SUCCESS, actionType, "팀 '$name'이(가) 수정되었습니다.", target = "team")
    }

    private fun handleDelete(
        action: ActionRequest,
        actionType: ActionType,
    ): ActionResult {
        val id =
            action.id
                ?: return ActionResult(ActionResultType.ERROR, actionType, "삭제할 팀 ID가 필요합니다.", target = "team")
        return ActionResult(
            type = ActionResultType.NEEDS_CONFIRM,
            action = actionType,
            message = "팀(ID: $id)을(를) 삭제하시겠습니까?",
            data = mapOf("teamId" to id),
            target = "team",
        )
    }
}
