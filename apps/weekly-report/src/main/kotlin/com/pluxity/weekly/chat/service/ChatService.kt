package com.pluxity.weekly.chat.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.context.ContextBuilder
import com.pluxity.weekly.chat.dto.ChatActionResponse
import com.pluxity.weekly.chat.llm.LlmService
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
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
    /**
     * read              → 서버 조회 → readResult
     * clarify           → 예외 (LLM message)
     * project/epic create → dto + selectFields (프론트에서 POST)
     * CUD + missingFields → 예외 (LLM message)
     * 나머지 CUD (확정)  → 서버 실행 → id 반환
     */
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
        val actions = llmService.generate(prompt).take(1)
        log.info { "LLM 응답 액션:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actions)}" }

        // LlmAction → ChatActionResponse 변환
        return actions.map { action ->
            val target = action.target ?: "task"
            when {
                // read → 서버 조회
                action.action == "read" -> ChatActionResponse(
                    action = action.action,
                    target = target,
                    readResult = chatReadHandler.handle(action),
                )
                // clarify → 예외
                action.action == "clarify" -> throw CustomException(
                    WeeklyReportErrorCode.LLM_AMBIGUOUS_REQUEST,
                    action.message ?: "좀 더 구체적으로 말씀해주세요.",
                )
                // project/epic create → 항상 dto + selectFields
                action.action == "create" && target in listOf("project", "epic") -> {
                    val selectFields = selectFieldResolver.resolve(action)
                    ChatActionResponse(
                        action = action.action,
                        target = target,
                        dto = chatDtoMapper.toDto(action),
                        selectFields = selectFields.ifEmpty { null },
                    )
                }
                // CUD + missingFields 또는 update/delete인데 id 없음 → 예외
                !action.missingFields.isNullOrEmpty() ||
                    (action.action in listOf("update", "delete") && action.id == null) ->
                    throw CustomException(
                        WeeklyReportErrorCode.LLM_AMBIGUOUS_REQUEST,
                        action.message ?: "대상을 특정할 수 없습니다.",
                    )
                // 나머지 CUD (확정) → 서버 실행
                else -> {
                    val resultId = chatExecutor.execute(action)
                    ChatActionResponse(
                        action = action.action,
                        target = target,
                        id = resultId,
                    )
                }
            }
        }
    }
}
