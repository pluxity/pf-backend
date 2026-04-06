package com.pluxity.safers.chat.service

import com.pluxity.common.auth.authentication.security.CustomUserDetails
import com.pluxity.safers.chat.dto.A2uiMessage
import com.pluxity.safers.chat.dto.BeginRenderingMessage
import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.dto.DataModelUpdateMessage
import com.pluxity.safers.chat.dto.IntentMode
import com.pluxity.safers.chat.dto.IntentResult
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.SurfaceUpdate
import com.pluxity.safers.chat.dto.SurfaceUpdateMessage
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
            val screenMetaList = chatHistoryStore.loadScreenMetaList(userId)

            // 1차 LLM: 의도 파악
            val intentPrompt = promptBuilder.buildIntentPrompt(history, screenMetaList)
            val intentMessages =
                listOf(
                    Message(role = "system", content = intentPrompt),
                    Message(role = "user", content = message),
                )
            val intentResult = chatLlmClient.analyzeIntent(intentMessages)

            val response =
                when (intentResult.mode) {
                    IntentMode.RECALL -> handleRecall(userId, intentResult, message)
                    IntentMode.MODIFY -> handleModify(userId, intentResult, message)
                    IntentMode.NEW -> handleNew(userId, intentResult, message)
                }

            // 히스토리 저장
            val turnNumber = chatHistoryStore.incrementTurn(userId)
            chatHistoryStore.save(
                userId,
                "system",
                "--- 히스토리 #$turnNumber | 질문: $message | 결과: ${intentResult.summary} | mode=${intentResult.mode} ---",
            )

            response
        } catch (e: Exception) {
            log.error(e) { "채팅 처리 실패: $message" }
            buildFallbackResponse(message)
        }
    }

    private fun handleNew(
        userId: String,
        intentResult: IntentResult,
        message: String,
    ): ChatResponse {
        val actions = intentResult.actions
        val dataModel = runBlocking { actionExecutor.execute(actions) }
        val response = generateLayout(message, dataModel)

        // 화면 캐시 저장 (actions가 있을 때만)
        if (actions.isNotEmpty()) {
            val ref = chatHistoryStore.nextScreenRef(userId)
            chatHistoryStore.saveScreen(userId, ref, intentResult.summary, actions, response)
        }

        return response
    }

    private fun handleRecall(
        userId: String,
        intentResult: IntentResult,
        message: String,
    ): ChatResponse {
        val ref = intentResult.ref
        if (ref == null) {
            log.warn { "recall 모드인데 ref가 없음, new로 폴백: $message" }
            return handleNew(userId, intentResult.copy(mode = IntentMode.NEW), message)
        }

        val cached = chatHistoryStore.loadScreen(userId, ref)
        if (cached == null) {
            log.warn { "캐시된 화면을 찾을 수 없음: ref=$ref, new로 폴백" }
            return handleNew(userId, intentResult.copy(mode = IntentMode.NEW, actions = intentResult.actions), message)
        }

        log.info { "화면 복원: ref=$ref, summary=${cached.meta.summary}" }
        return cached.response
    }

    private fun handleModify(
        userId: String,
        intentResult: IntentResult,
        message: String,
    ): ChatResponse {
        val ref = intentResult.ref
        val patch = intentResult.patch

        if (ref == null || patch == null) {
            log.warn { "modify 모드인데 ref 또는 patch가 없음, new로 폴백: $message" }
            return handleNew(userId, intentResult.copy(mode = IntentMode.NEW), message)
        }

        val cached = chatHistoryStore.loadScreen(userId, ref)
        if (cached == null) {
            log.warn { "캐시된 화면을 찾을 수 없음: ref=$ref, new로 폴백" }
            return handleNew(userId, intentResult.copy(mode = IntentMode.NEW), message)
        }

        // 이전 actions에서 remove 대상 제거 후 add 대상 추가
        val removeIds = patch.remove.toSet()
        val mergedActions: List<QueryAction> =
            cached.actions.filter { it.id !in removeIds } + patch.add

        val dataModel = runBlocking { actionExecutor.execute(mergedActions) }
        val response = generateLayout(message, dataModel)

        // 수정된 화면도 새 ref로 캐시
        val newRef = chatHistoryStore.nextScreenRef(userId)
        chatHistoryStore.saveScreen(userId, newRef, intentResult.summary, mergedActions, response)

        return response
    }

    private fun generateLayout(
        message: String,
        dataModel: Map<String, Any>,
    ): ChatResponse {
        val dataSummary = promptBuilder.buildDataSummary(message, dataModel)
        val layoutMessages =
            listOf(
                Message(role = "system", content = promptBuilder.buildLayoutPrompt()),
                Message(role = "user", content = dataSummary),
            )
        val surfaceUpdate = chatLlmClient.generateLayout(layoutMessages)
        return buildResponse(surfaceUpdate, dataModel)
    }

    private fun buildResponse(
        surfaceUpdate: SurfaceUpdate,
        dataModel: Map<String, Any>,
    ): ChatResponse {
        val surfaceId = surfaceUpdate.surfaceId
        val messages =
            listOf(
                A2uiMessage(
                    surfaceUpdate =
                        SurfaceUpdateMessage(surfaceId = surfaceId, components = surfaceUpdate.components),
                ),
                A2uiMessage(
                    dataModelUpdate =
                        DataModelUpdateMessage(surfaceId = surfaceId, contents = dataModel),
                ),
                A2uiMessage(
                    beginRendering =
                        BeginRenderingMessage(surfaceId = surfaceId, root = "root", catalogId = "safers"),
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
        val surfaceId = "fallback"
        val components =
            listOf(
                mapOf(
                    "id" to "msg",
                    "component" to
                        mapOf(
                            "AgentMessageCard" to
                                mapOf("message" to "죄송합니다. 요청을 처리하지 못했습니다: $message"),
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
            )
        return ChatResponse(
            messages =
                listOf(
                    A2uiMessage(
                        surfaceUpdate = SurfaceUpdateMessage(surfaceId = surfaceId, components = components),
                    ),
                    A2uiMessage(
                        beginRendering = BeginRenderingMessage(surfaceId = surfaceId, root = "root", catalogId = "safers"),
                    ),
                ),
        )
    }
}
