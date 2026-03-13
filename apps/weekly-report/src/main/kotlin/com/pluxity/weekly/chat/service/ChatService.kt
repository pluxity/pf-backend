package com.pluxity.weekly.chat.service

import com.pluxity.weekly.chat.action.ActionHandler
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.LlmAction
import com.pluxity.weekly.chat.action.dto.ResolveAction
import com.pluxity.weekly.chat.context.ContextBuilder
import com.pluxity.weekly.chat.dto.ChatResponse
import com.pluxity.weekly.chat.llm.LlmService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val llmService: LlmService,
    private val contextBuilder: ContextBuilder,
    private val actionHandler: ActionHandler,
    private val objectMapper: ObjectMapper,
) {
    fun chat(
        message: String,
        userId: Long,
    ): ChatResponse {
        // 1차: 의도 추출 (context 없음, 가벼움)
        val intent = llmService.extractIntent(message)
        log.info { "1차 의도 추출 - action: ${intent.actions}, target: ${intent.target}" }

        // project/team create는 부모 없음 → 2차 LLM 스킵, 바로 폼 반환
        if (intent.actions.size == 1 &&
            intent.actions.first() == "create" &&
            intent.target in listOf("project", "team")
        ) {
            // 권한 체크 필요

            val action =
                LlmAction(
                    action = "create",
                    target = intent.target,
                    name = intent.name,
                )
            val result = actionHandler.handle(action, userId)
            return ChatResponse(results = listOf(result))
        }

        // target별+권한별 context 조회
        val context = contextBuilder.build(userId, intent.target)
        log.info { "LLM 컨텍스트:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(context))}" }

        // 2차: 완성형 LlmAction 생성 (context + intent 포함)
        val intentJson = objectMapper.writeValueAsString(intent)
        val prompt = "[INTENT]\n$intentJson\n[/INTENT]\n\n[CONTEXT]\n$context\n[/CONTEXT]\n\n$message"
        log.info { "LLM 요청 - 사용자: $userId, 메시지: $message" }

        val actions = llmService.generate(prompt)
        log.info { "LLM 응답 액션:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actions)}" }

        // 실행 (Service의 @CheckPermission이 권한 체크)
        val results = actions.map { actionHandler.handle(it, userId) }
        return ChatResponse(results = results)
    }

    fun resolve(
        partial: Map<String, Any?>,
        selected: Map<String, String>,
        userId: Long,
    ): ActionResult {
        val resolveAction =
            ResolveAction(
                action = partial["action"] as? String ?: "read",
                target = partial["target"] as? String,
                project = selected["project"] ?: partial["project"] as? String,
                projectId = (selected["projectId"] ?: partial["projectId"])?.toString()?.toLongOrNull(),
                epic = selected["epic"] ?: partial["epic"] as? String,
                name = selected["name"] ?: partial["name"] as? String,
                description = selected["description"] ?: partial["description"] as? String,
                status = selected["status"] ?: partial["status"] as? String,
                progress = (partial["progress"] as? Number)?.toInt(),
                startDate = selected["startDate"] ?: partial["startDate"] as? String,
                dueDate = selected["dueDate"] ?: partial["dueDate"] as? String,
                pmId = (selected["pmId"] ?: partial["pmId"])?.toString()?.toLongOrNull(),
                filters = partial["filters"] as? Map<String, Any?>,
            )

        log.info { "Resolve 요청 - partial: $partial, selected: $selected" }
        return actionHandler.handle(resolveAction, userId)
    }
}
