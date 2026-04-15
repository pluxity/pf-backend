package com.pluxity.safers.chat.service

import com.pluxity.safers.cctv.service.CctvService
import com.pluxity.safers.chat.A2uiConstants
import com.pluxity.safers.chat.dto.A2uiMessage
import com.pluxity.safers.chat.dto.ActionResult
import com.pluxity.safers.chat.dto.BeginRenderingMessage
import com.pluxity.safers.chat.dto.ChatActionRequest
import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.dto.DataModelUpdateMessage
import com.pluxity.safers.chat.dto.IntentMode
import com.pluxity.safers.chat.dto.IntentResult
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.SurfaceUpdateMessage
import com.pluxity.safers.chat.prompt.ChatPromptBuilder
import com.pluxity.safers.llm.ChatLlmClient
import com.pluxity.safers.llm.dto.Message
import com.pluxity.safers.site.dto.SiteSummary
import com.pluxity.safers.site.service.SiteService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val chatLlmClient: ChatLlmClient,
    private val actionExecutor: ChatActionExecutor,
    private val chatHistoryStore: ChatHistoryStore,
    private val siteService: SiteService,
    private val cctvService: CctvService,
) {
    private val promptBuilder = ChatPromptBuilder()

    fun chat(
        userId: String,
        message: String,
    ): ChatResponse =
        try {
            val history = chatHistoryStore.load(userId)
            val screenMetaList = chatHistoryStore.loadScreenMetaList(userId)
            val sites = siteService.findAllSites()

            // 1차 LLM: 의도 파악
            val intentPrompt = promptBuilder.buildIntentPrompt(sites, history, screenMetaList)
            val intentMessages =
                listOf(
                    Message(role = "system", content = intentPrompt),
                    Message(role = "user", content = message),
                )
            val intentResult = chatLlmClient.analyzeIntent(intentMessages)
            val turnNumber = chatHistoryStore.incrementTurn(userId)

            val response =
                when (intentResult.mode) {
                    IntentMode.RECALL -> handleRecall(userId, intentResult, message)
                    IntentMode.MODIFY -> handleModify(userId, intentResult, message, turnNumber, sites)
                    IntentMode.NEW -> handleNew(userId, intentResult, message, turnNumber, sites)
                }

            // 히스토리 저장
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

    fun handleAction(request: ChatActionRequest): ChatResponse {
        val result = actionExecutor.executeAction(request)
        val dataModel = mapOf(request.actionId to result)
        return buildDataOnlyResponse(dataModel)
    }

    private fun handleNew(
        userId: String,
        intentResult: IntentResult,
        message: String,
        turnNumber: Long,
        sites: List<SiteSummary>,
    ): ChatResponse {
        val actions = intentResult.actions
        val dataModel = runBlocking { actionExecutor.execute(actions) }
        val response = generateLayout(message, dataModel, sites)

        // 화면 캐시 저장 (actions가 있을 때만)
        if (actions.isNotEmpty()) {
            chatHistoryStore.saveScreen(userId, "h$turnNumber", intentResult.summary, actions, response)
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
            log.warn { "recall 모드인데 ref가 없음: $message" }
            return buildFallbackResponse("요청하신 화면을 찾을 수 없습니다.")
        }

        val cached = chatHistoryStore.loadScreen(userId, ref)
        if (cached == null) {
            log.warn { "캐시된 화면을 찾을 수 없음: ref=$ref" }
            return buildFallbackResponse("이전 화면($ref)이 만료되었거나 존재하지 않습니다.")
        }

        log.info { "화면 복원: ref=$ref, summary=${cached.meta.summary}" }

        // 캐시된 레이아웃은 유지하고 데이터만 재조회
        val cachedSurfaceUpdate =
            cached.response.messages.firstNotNullOfOrNull { it.surfaceUpdate }
                ?: return buildFallbackResponse("이전 화면의 레이아웃을 복원할 수 없습니다.")
        val dataModel = runBlocking { actionExecutor.execute(cached.actions) }
        return buildResponse(cachedSurfaceUpdate, dataModel)
    }

    private fun handleModify(
        userId: String,
        intentResult: IntentResult,
        message: String,
        turnNumber: Long,
        sites: List<SiteSummary>,
    ): ChatResponse {
        val ref = intentResult.ref
        val patch = intentResult.patch

        if (ref == null || patch == null) {
            log.warn { "modify 모드인데 ref 또는 patch가 없음: $message" }
            return buildFallbackResponse("수정할 이전 화면을 찾을 수 없습니다.")
        }

        val cached = chatHistoryStore.loadScreen(userId, ref)
        if (cached == null) {
            log.warn { "캐시된 화면을 찾을 수 없음: ref=$ref" }
            return buildFallbackResponse("이전 화면($ref)이 만료되었거나 존재하지 않습니다.")
        }

        // 이전 actions에서 remove 대상 제거 후 add 대상 추가
        val removeIds = patch.remove.toSet()
        val mergedActions: List<QueryAction> =
            cached.actions.filter { it.id !in removeIds } + patch.add

        val dataModel = runBlocking { actionExecutor.execute(mergedActions) }
        val response = generateLayout(message, dataModel, sites)

        // 수정된 화면도 새 ref로 캐시
        chatHistoryStore.saveScreen(userId, "h$turnNumber", intentResult.summary, mergedActions, response)

        return response
    }

    private fun generateLayout(
        message: String,
        dataModel: Map<String, ActionResult>,
        sites: List<SiteSummary>,
    ): ChatResponse {
        val cctvs = cctvService.findAllSummaries()
        val dataSummary = promptBuilder.buildDataSummary(message, dataModel)
        val layoutMessages =
            listOf(
                Message(role = "system", content = promptBuilder.buildLayoutPrompt(sites, cctvs)),
                Message(role = "user", content = dataSummary),
            )
        val surfaceUpdate = chatLlmClient.generateLayout(layoutMessages)
        return buildResponse(surfaceUpdate, dataModel)
    }

    private fun buildResponse(
        surfaceUpdate: SurfaceUpdateMessage,
        dataModel: Map<String, ActionResult>,
    ): ChatResponse {
        val surfaceId = surfaceUpdate.surfaceId
        return ChatResponse(
            messages =
                listOf(
                    A2uiMessage(surfaceUpdate = surfaceUpdate),
                    A2uiMessage(
                        dataModelUpdate = DataModelUpdateMessage(surfaceId = surfaceId, contents = dataModel),
                    ),
                    A2uiMessage(
                        beginRendering =
                            BeginRenderingMessage(
                                surfaceId = surfaceId,
                                root = A2uiConstants.ROOT_ID,
                                catalogId = A2uiConstants.CATALOG_ID,
                            ),
                    ),
                ),
        )
    }

    private fun buildDataOnlyResponse(dataModel: Map<String, ActionResult>): ChatResponse =
        ChatResponse(
            messages =
                listOf(
                    A2uiMessage(
                        dataModelUpdate =
                            DataModelUpdateMessage(surfaceId = A2uiConstants.SURFACE_CURRENT, contents = dataModel),
                    ),
                ),
        )

    private fun buildFallbackResponse(message: String): ChatResponse {
        val surfaceId = A2uiConstants.SURFACE_FALLBACK
        val components =
            listOf(
                mapOf(
                    "id" to A2uiConstants.FALLBACK_MESSAGE_ID,
                    "component" to
                        mapOf(
                            "AgentMessageCard" to
                                mapOf("message" to "죄송합니다. 요청을 처리하지 못했습니다: $message"),
                        ),
                ),
                mapOf(
                    "id" to A2uiConstants.ROOT_ID,
                    "component" to
                        mapOf(
                            "FlexCol" to
                                mapOf(
                                    "children" to mapOf("explicitList" to listOf(A2uiConstants.FALLBACK_MESSAGE_ID)),
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
                        beginRendering =
                            BeginRenderingMessage(
                                surfaceId = surfaceId,
                                root = A2uiConstants.ROOT_ID,
                                catalogId = A2uiConstants.CATALOG_ID,
                            ),
                    ),
                ),
        )
    }
}
