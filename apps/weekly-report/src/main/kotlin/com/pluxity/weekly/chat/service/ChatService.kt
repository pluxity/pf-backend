package com.pluxity.weekly.chat.service

import com.pluxity.weekly.chat.action.ActionHandler
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.LlmAction
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
        val context = contextBuilder.build(userId)
        val prompt = "[CONTEXT]\n$context\n[/CONTEXT]\n\n$message"

        log.info { "LLM 컨텍스트:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(context))}" }
        log.info { "LLM 요청 - 사용자: $userId, 메시지: $message" }

        val actions = llmService.generate(prompt)

        log.info { "LLM 응답 액션:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actions)}" }

        val results = actions.map { actionHandler.handle(it, userId) }
        return ChatResponse(results = results)
    }

    fun resolve(
        partial: Map<String, Any?>,
        selected: Map<String, String>,
        userId: Long,
    ): ActionResult {
        val llmAction =
            LlmAction(
                action = partial["action"] as? String ?: "read",
                project = selected["project"],
                epic = selected["epic"],
                name = selected["name"],
                status = partial["status"] as? String,
                progress = (partial["progress"] as? Number)?.toInt(),
                filters = partial["filters"] as? Map<String, Any?>,
            )

        log.info { "Resolve 요청 - partial: $partial, selected: $selected" }
        return actionHandler.handle(llmAction, userId)
    }
}
