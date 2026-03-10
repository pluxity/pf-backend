package com.pluxity.weekly.chat.service

import com.pluxity.weekly.chat.action.ActionHandler
import com.pluxity.weekly.chat.action.dto.ActionResult
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.action.dto.ActionType
import com.pluxity.weekly.chat.action.dto.LlmAction
import com.pluxity.weekly.chat.context.ContextBuilder
import com.pluxity.weekly.chat.llm.LlmService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

class ChatServiceTest :
    BehaviorSpec({

        val llmService: LlmService = mockk()
        val contextBuilder: ContextBuilder = mockk()
        val actionHandler: ActionHandler = mockk()
        val objectMapper: ObjectMapper = JsonMapper.builder().build()

        val chatService = ChatService(llmService, contextBuilder, actionHandler, objectMapper)

        val userId = 1L

        Given("채팅 플로우") {
            When("사용자가 메시지를 보내면") {
                val contextJson = """{"today":"2026-03-09","user":{"id":1,"name":"홍길동"},"projects":[]}"""
                val llmAction =
                    LlmAction(
                        action = "read",
                        filters = mapOf("status" to "IN_PROGRESS"),
                    )
                val actionResult =
                    ActionResult(
                        type = ActionResultType.SUCCESS,
                        action = ActionType.READ,
                        message = "2개의 태스크를 조회했습니다.",
                        data = emptyList<Any>(),
                    )

                every { contextBuilder.build(userId) } returns contextJson
                every { llmService.generate(any()) } returns listOf(llmAction)
                every { actionHandler.handle(llmAction, userId) } returns actionResult

                val response = chatService.chat("진행중인 태스크 보여줘", userId)

                Then("컨텍스트 빌드 → LLM 호출 → 액션 처리 순으로 실행된다") {
                    verify(exactly = 1) { contextBuilder.build(userId) }
                    verify(exactly = 1) { llmService.generate(any()) }
                    verify(exactly = 1) { actionHandler.handle(llmAction, userId) }
                }

                Then("결과가 올바르게 반환된다") {
                    response.results.size shouldBe 1
                    response.results[0].type shouldBe ActionResultType.SUCCESS
                    response.results[0].message shouldBe "2개의 태스크를 조회했습니다."
                }
            }
        }

        Given("resolve 플로우") {
            When("clarify 후 사용자가 candidate를 선택하면") {
                val partial = mapOf<String, Any?>("action" to "delete")
                val selected = "SAFERS > api 구현 > api 명세서 작성"
                val actionResult =
                    ActionResult(
                        type = ActionResultType.NEEDS_CONFIRM,
                        action = ActionType.DELETE,
                        message = "태스크(ID: 1)을(를) 삭제하시겠습니까?",
                        data = mapOf("taskId" to 1L),
                    )

                every { actionHandler.handle(any(), userId) } returns actionResult

                val result = chatService.resolve(partial, selected, userId)

                Then("partial과 selected가 합쳐져서 액션이 실행된다") {
                    result.type shouldBe ActionResultType.NEEDS_CONFIRM
                    verify(exactly = 1) { actionHandler.handle(any(), userId) }
                }
            }

            When("partial에 status 등 추가 정보가 있으면") {
                val partial = mapOf<String, Any?>("action" to "read", "status" to "IN_PROGRESS")
                val selected = "SAFERS > api 구현"
                val actionResult =
                    ActionResult(
                        type = ActionResultType.SUCCESS,
                        action = ActionType.READ,
                        message = "1개의 태스크를 조회했습니다.",
                    )

                every { actionHandler.handle(any(), userId) } returns actionResult

                val result = chatService.resolve(partial, selected, userId)

                Then("추가 정보가 포함되어 실행된다") {
                    result.type shouldBe ActionResultType.SUCCESS
                }
            }
        }
    })
