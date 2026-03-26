package com.pluxity.weekly.teams.converter

import com.pluxity.weekly.chat.dto.ChatActionResponse
import com.pluxity.weekly.chat.dto.ChatDto
import com.pluxity.weekly.chat.dto.ChatReadResponse
import com.pluxity.weekly.chat.dto.EpicChatDto
import com.pluxity.weekly.chat.dto.ProjectChatDto
import com.pluxity.weekly.chat.dto.SelectField
import com.pluxity.weekly.chat.dto.TaskChatDto
import com.pluxity.weekly.chat.dto.TeamChatDto
import com.pluxity.weekly.epic.dto.EpicResponse
import com.pluxity.weekly.project.dto.ProjectResponse
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.team.dto.TeamResponse
import org.springframework.stereotype.Component

@Component
class AdaptiveCardConverter {
    fun toTeamsResponse(response: ChatActionResponse): Map<String, Any> {
        // 서버 실행 완료 (id 있음)
        if (response.id != null) {
            return textMessage(formatExecutionResult(response))
        }

        // 조회 결과
        if (response.readResult != null) {
            return adaptiveCard(buildReadCard(response.readResult, response.target))
        }

        // 생성/수정 폼
        if (response.dto != null) {
            return adaptiveCard(buildFormCard(response))
        }

        return textMessage("처리가 완료되었습니다.")
    }

    fun textMessage(text: String): Map<String, Any> =
        mapOf(
            "type" to "message",
            "text" to text,
        )

    fun adaptiveCard(card: Map<String, Any>): Map<String, Any> =
        mapOf(
            "type" to "message",
            "attachments" to
                listOf(
                    mapOf(
                        "contentType" to "application/vnd.microsoft.card.adaptive",
                        "content" to card,
                    ),
                ),
        )

    private fun formatExecutionResult(response: ChatActionResponse): String {
        val actionLabel =
            when (response.action) {
                "create" -> "생성"
                "update" -> "수정"
                "delete" -> "삭제"
                else -> response.action
            }
        val targetLabel = targetLabel(response.target)
        return "$targetLabel ${actionLabel}이 완료되었습니다. (ID: ${response.id})"
    }

    private fun buildReadCard(
        readResult: ChatReadResponse,
        target: String,
    ): Map<String, Any> {
        val facts =
            when {
                !readResult.tasks.isNullOrEmpty() -> readResult.tasks.map { it.toFact() }
                !readResult.projects.isNullOrEmpty() -> readResult.projects.map { it.toFact() }
                !readResult.epics.isNullOrEmpty() -> readResult.epics.map { it.toFact() }
                !readResult.teams.isNullOrEmpty() -> readResult.teams.map { it.toFact() }
                else -> return emptyCard("조회 결과가 없습니다.")
            }

        return mapOf(
            "type" to "AdaptiveCard",
            "version" to "1.2",
            "body" to
                listOf(
                    mapOf(
                        "type" to "TextBlock",
                        "text" to "${targetLabel(target)} 조회 결과",
                        "weight" to "bolder",
                        "size" to "medium",
                    ),
                    mapOf(
                        "type" to "FactSet",
                        "facts" to facts,
                    ),
                ),
        )
    }

    private fun buildFormCard(response: ChatActionResponse): Map<String, Any> {
        val dto = response.dto!!
        val inputs = buildInputs(dto, response.selectFields.orEmpty())
        val actionLabel = if (response.action == "create") "생성" else "수정"

        return mapOf(
            "type" to "AdaptiveCard",
            "version" to "1.2",
            "body" to listOf(
                mapOf(
                    "type" to "TextBlock",
                    "text" to "${targetLabel(response.target)} $actionLabel",
                    "weight" to "bolder",
                    "size" to "medium",
                ),
            ) + inputs,
            "actions" to
                listOf(
                    mapOf(
                        "type" to "Action.Submit",
                        "title" to actionLabel,
                        "data" to
                            mapOf(
                                "action" to response.action,
                                "target" to response.target,
                            ),
                    ),
                ),
        )
    }

    private fun buildInputs(
        dto: ChatDto,
        selectFields: List<SelectField>,
    ): List<Map<String, Any>> {
        val selectFieldMap = selectFields.associateBy { it.field }
        val inputs = mutableListOf<Map<String, Any>>()

        when (dto) {
            is ProjectChatDto -> {
                inputs += textInput("name", "프로젝트명", dto.name)
                inputs += textInput("description", "설명", dto.description)
                inputs += selectOrText("pmId", "PM", selectFieldMap)
                inputs += textInput("startDate", "시작일", dto.startDate)
                inputs += textInput("dueDate", "마감일", dto.dueDate)
            }
            is EpicChatDto -> {
                inputs += textInput("name", "에픽명", dto.name)
                inputs += selectOrText("projectId", "프로젝트", selectFieldMap)
                inputs += textInput("description", "설명", dto.description)
                inputs += textInput("startDate", "시작일", dto.startDate)
                inputs += textInput("dueDate", "마감일", dto.dueDate)
            }
            is TaskChatDto -> {
                inputs += textInput("name", "태스크명", dto.name)
                inputs += selectOrText("epicId", "에픽", selectFieldMap)
                inputs += textInput("description", "설명", dto.description)
                inputs += textInput("startDate", "시작일", dto.startDate)
                inputs += textInput("dueDate", "마감일", dto.dueDate)
            }
            is TeamChatDto -> {
                inputs += textInput("name", "팀명", dto.name)
                inputs += selectOrText("leaderId", "팀장", selectFieldMap)
            }
        }

        return inputs
    }

    private fun textInput(
        id: String,
        label: String,
        value: String?,
    ): Map<String, Any> {
        val input =
            mutableMapOf<String, Any>(
                "type" to "Input.Text",
                "id" to id,
                "label" to label,
            )
        if (value != null) input["value"] = value
        return input
    }

    private fun selectOrText(
        fieldId: String,
        label: String,
        selectFieldMap: Map<String, SelectField>,
    ): Map<String, Any> {
        val selectField = selectFieldMap[fieldId]
        return if (selectField != null && selectField.candidates.isNotEmpty()) {
            mapOf(
                "type" to "Input.ChoiceSet",
                "id" to fieldId,
                "label" to label,
                "choices" to
                    selectField.candidates.map { candidate ->
                        mapOf("title" to candidate.name, "value" to candidate.id)
                    },
            )
        } else {
            textInput(fieldId, label, null)
        }
    }

    private fun emptyCard(message: String): Map<String, Any> =
        mapOf(
            "type" to "AdaptiveCard",
            "version" to "1.2",
            "body" to
                listOf(
                    mapOf("type" to "TextBlock", "text" to message),
                ),
        )

    private fun targetLabel(target: String): String =
        when (target) {
            "project" -> "프로젝트"
            "epic" -> "에픽"
            "task" -> "태스크"
            "team" -> "팀"
            else -> target
        }

    private fun TaskResponse.toFact(): Map<String, String> = mapOf("title" to name, "value" to "$status ($progress%)")

    private fun ProjectResponse.toFact(): Map<String, String> = mapOf("title" to name, "value" to "$status")

    private fun EpicResponse.toFact(): Map<String, String> = mapOf("title" to name, "value" to "$status")

    private fun TeamResponse.toFact(): Map<String, String> = mapOf("title" to name, "value" to (leaderName ?: "-"))
}
