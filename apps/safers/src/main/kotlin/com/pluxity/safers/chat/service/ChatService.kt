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
import com.pluxity.safers.llm.LlmClient
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

            // 1차 LLM: 의도 파악 — 히스토리를 시스템 프롬프트에 포함시켜 system+user 2개 메시지만 전송
            val intentPrompt = promptBuilder.buildIntentPrompt(history)
            val intentMessages =
                listOf(
                    Message(role = "system", content = intentPrompt),
                    Message(role = "user", content = message),
                )
            val intentResult = chatLlmClient.analyzeIntent(intentMessages)

            // 데이터 조회
            val dataModel =
                runBlocking {
                    actionExecutor.execute(intentResult.actions)
                }

            // 2차 LLM: 데이터 기반 UI 배치
            val dataSummary = promptBuilder.buildDataSummary(message, dataModel)
            val layoutMessages =
                listOf(
                    Message(role = "system", content = promptBuilder.buildLayoutPrompt()),
                    Message(role = "user", content = dataSummary),
                )
            val surfaceUpdate = chatLlmClient.generateLayout(layoutMessages)

            // 히스토리 저장 — system role + 구분된 포맷으로 저장하여 LLM이 응답 형식으로 모방하지 않게 함
            val turnNumber = chatHistoryStore.incrementTurn(userId)
            val actionsJson = LlmClient.objectMapper.writeValueAsString(intentResult.actions)
            chatHistoryStore.save(userId, "user", message)
            chatHistoryStore.save(
                userId,
                "system",
                "--- 히스토리 #$turnNumber: ${intentResult.summary} | actions=$actionsJson ---",
            )

            buildResponse(surfaceUpdate, dataModel)
        } catch (e: Exception) {
            log.error(e) { "채팅 처리 실패: $message" }
            buildFallbackResponse(message)
        }
    }

    private fun buildResponse(
        surfaceUpdate: SurfaceUpdate,
        dataModel: Map<String, Any>,
    ): ChatResponse {
        val surfaceId = surfaceUpdate.surfaceId
        val messages =
            listOf(
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

    private fun buildFallbackResponse(message: String): ChatResponse =
        ChatResponse(
            messages =
                listOf(
                    A2uiMessage(
                        createSurface = CreateSurface(surfaceId = "fallback", catalogId = "safers"),
                    ),
                    A2uiMessage(
                        updateComponents =
                            UpdateComponents(
                                surfaceId = "fallback",
                                components =
                                    listOf(
                                        mapOf(
                                            "id" to "msg",
                                            "component" to
                                                mapOf(
                                                    "AgentMessageCard" to
                                                        mapOf(
                                                            "message" to "죄송합니다. 요청을 처리하지 못했습니다: $message",
                                                        ),
                                                ),
                                        ),
                                        mapOf(
                                            "id" to "root",
                                            "component" to
                                                mapOf(
                                                    "FlexCol" to
                                                        mapOf(
                                                            "children" to mapOf("explicitList" to listOf("msg")),
                                                            "gap" to 0,
                                                        ),
                                                ),
                                        ),
                                    ),
                            ),
                    ),
                ),
        )
}
