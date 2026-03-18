package com.pluxity.weekly.chat.service

import com.pluxity.weekly.chat.context.ContextBuilder
import com.pluxity.weekly.chat.dto.ChatActionResponse
import com.pluxity.weekly.chat.llm.LlmService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val llmService: LlmService,
    private val contextBuilder: ContextBuilder,
    private val chatDtoMapper: ChatDtoMapper,
    private val selectFieldResolver: SelectFieldResolver,
    private val chatReadHandler: ChatReadHandler,
    private val chatExecutor: ChatExecutor,
    private val objectMapper: ObjectMapper,
) {
    fun chat(
        message: String,
        userId: Long,
    ): List<ChatActionResponse> {
        // 1차: 의도 추출
        val intent = llmService.extractIntent(message)
        log.info { "1차 의도 추출 - action: ${intent.actions}, target: ${intent.target}" }

        // target별+action별+권한별 context 조회
        val context = contextBuilder.build(userId, intent.target, intent.actions)
        log.info { "context:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(context))}" }

        // 2차: LlmAction 생성
        val intentJson = objectMapper.writeValueAsString(intent)
        val prompt = "[INTENT]\n$intentJson\n[/INTENT]\n\n[CONTEXT]\n$context\n[/CONTEXT]\n\n$message"
        val actions = llmService.generate(prompt)
        log.info { "LLM 응답 액션:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actions)}" }

        // LlmAction → ChatActionResponse 변환
        return actions.map { action ->
            if (action.action == "read") {
                ChatActionResponse(
                    action = action.action,
                    target = action.target ?: "task",
                    readResult = chatReadHandler.handle(action),
                )
            } else {
                val beforeAction = beforeActionResolver.resolve(action)
                ChatActionResponse(
                    action = action.action,
                    target = action.target ?: "task",
                    id = action.id,
                    dto = chatDtoMapper.toDto(action),
                    beforeAction = beforeAction.ifEmpty { null },
                )
            }
        }
    }
}
