package com.pluxity.safers.chat.service

import com.pluxity.common.auth.authentication.security.CustomUserDetails
import com.pluxity.safers.chat.dto.A2uiMessage
import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.dto.CreateSurface
import com.pluxity.safers.chat.dto.SurfaceUpdate
import com.pluxity.safers.chat.dto.UpdateComponents
import com.pluxity.safers.chat.dto.UpdateDataModel
import com.pluxity.safers.chat.prompt.ChatPromptBuilder
import com.pluxity.safers.llm.ChatLlmClient
import com.pluxity.safers.llm.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val chatLlmClient: ChatLlmClient,
    private val actionExecutor: ChatActionExecutor,
    private val promptBuilder: ChatPromptBuilder,
    private val chatHistoryStore: ChatHistoryStore,
) {
    fun chat(message: String): ChatResponse {
        val userId = getCurrentUserId()
        return try {
            val history = chatHistoryStore.load(userId)

            // 1차 LLM: 의도 파악 (actions)
            val intentPrompt = promptBuilder.buildIntentPrompt()
            val intentMessages = buildList {
                add(Message(role = "system", content = intentPrompt))
                addAll(history.takeLast(10))
                add(Message(role = "user", content = message))
            }
            val intentResult = chatLlmClient.analyzeIntent(intentMessages)

            // 데이터 조회
            val dataModel = runBlocking {
                actionExecutor.execute(intentResult.actions)
            }

            // 2차 LLM: 데이터 기반 UI 배치
            val dataSummary = promptBuilder.buildDataSummary(message, dataModel)
            val layoutMessages = listOf(
                Message(role = "system", content = promptBuilder.buildLayoutPrompt()),
                Message(role = "user", content = dataSummary),
            )
            val surfaceUpdate = chatLlmClient.generateLayout(layoutMessages)

            // 히스토리 저장
            val actionsDesc = intentResult.actions.joinToString(", ") { "${it.target} ${it.filters}" }
            val historyContent = "응답: ${intentResult.summary} (actions: $actionsDesc)"
            chatHistoryStore.save(userId, "user", message)
            chatHistoryStore.save(userId, "assistant", historyContent)

            buildResponse(surfaceUpdate, dataModel)
        } catch (e: Exception) {
            log.error(e) { "채팅 처리 실패: $message" }
            buildFallbackResponse(message)
        }
    }

    private fun buildResponse(surfaceUpdate: SurfaceUpdate, dataModel: Map<String, Any>): ChatResponse {
        val surfaceId = surfaceUpdate.surfaceId
        val messages = listOf(
            A2uiMessage(
                createSurface = CreateSurface(surfaceId = surfaceId, catalogId = "safers"),
            ),
            A2uiMessage(
                updateComponents = UpdateComponents(surfaceId = surfaceId, components = surfaceUpdate.components),
            ),
            A2uiMessage(
                updateDataModel = UpdateDataModel(surfaceId = surfaceId, value = dataModel),
            ),
        )
        return ChatResponse(messages = messages)
    }

    private fun getCurrentUserId(): String {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return when (principal) {
            is CustomUserDetails -> principal.user.requiredId.toString()
            else -> "anonymous"
        }
    }

    private fun buildFallbackResponse(message: String): ChatResponse {
        return ChatResponse(
            messages = listOf(
                A2uiMessage(
                    createSurface = CreateSurface(surfaceId = "fallback", catalogId = "safers"),
                ),
                A2uiMessage(
                    updateComponents = UpdateComponents(
                        surfaceId = "fallback",
                        components = listOf(
                            mapOf(
                                "id" to "msg",
                                "component" to mapOf("AgentMessageCard" to mapOf("message" to "죄송합니다. 요청을 처리하지 못했습니다: $message")),
                            ),
                            mapOf(
                                "id" to "root",
                                "component" to mapOf("FlexCol" to mapOf("children" to mapOf("explicitList" to listOf("msg")), "gap" to 0)),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
