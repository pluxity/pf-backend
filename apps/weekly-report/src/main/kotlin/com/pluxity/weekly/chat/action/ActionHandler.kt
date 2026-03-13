package com.pluxity.weekly.chat.action

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.action.dto.ActionRequest
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.LlmAction
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ActionHandler(
    private val taskActionHandler: TaskActionHandler,
    private val projectActionHandler: ProjectActionHandler,
    private val epicActionHandler: EpicActionHandler,
    private val teamActionHandler: TeamActionHandler,
) {
    fun handle(
        action: ActionRequest,
        userId: Long,
    ): ActionResult {
        val actionType = resolveActionType(action.action)
        val target = action.target ?: "task"
        return try {
            when (actionType) {
                ActionType.CLARIFY -> handleClarify(action as LlmAction, actionType, target)
                ActionType.UNKNOWN -> ActionResult(ActionResultType.ERROR, actionType, "알 수 없는 액션: ${action.action}", target = target)
                else ->
                    when (target) {
                        "task" -> taskActionHandler.handle(action, userId, actionType)
                        "project" -> projectActionHandler.handle(action, userId, actionType)
                        "epic" -> epicActionHandler.handle(action, userId, actionType)
                        "team" -> teamActionHandler.handle(action, userId, actionType)
                        else -> ActionResult(ActionResultType.ERROR, actionType, "알 수 없는 대상: $target", target = target)
                    }
            }
        } catch (e: CustomException) {
            log.warn { "액션 처리 중 권한/비즈니스 오류: ${e.message}" }
            ActionResult(ActionResultType.ERROR, actionType, e.message ?: "처리 중 오류가 발생했습니다.", target = target)
        }
    }

    private fun resolveActionType(action: String): ActionType =
        when (action) {
            "read" -> ActionType.READ
            "create" -> ActionType.CREATE
            "update" -> ActionType.UPDATE
            "delete" -> ActionType.DELETE
            "clarify" -> ActionType.CLARIFY
            else -> ActionType.UNKNOWN
        }

    private fun handleClarify(
        action: LlmAction,
        actionType: ActionType,
        target: String,
    ): ActionResult =
        ActionResult(
            type = ActionResultType.CLARIFY,
            action = actionType,
            message = action.message ?: "추가 정보가 필요합니다.",
            candidates = action.candidates,
            partial = action.partial,
            target = target,
        )
}
