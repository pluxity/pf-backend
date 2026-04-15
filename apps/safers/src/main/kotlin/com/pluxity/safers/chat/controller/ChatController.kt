package com.pluxity.safers.chat.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.chat.dto.ChatActionRequest
import com.pluxity.safers.chat.dto.ChatRequest
import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.service.ChatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat Controller", description = "자연어 채팅 API (A2UI)")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping
    @Operation(
        summary = "자연어 질문으로 데이터 조회 및 A2UI 응답 생성",
        description =
            "사용자 자연어 질문을 받아 (1) 의도 파악 LLM, (2) 데이터 병렬 조회, (3) UI 레이아웃 LLM 순으로 처리한다. " +
                "응답은 A2UI v0.8 메시지 스트림이며, 액션별 데이터 조회 실패는 HTTP 200 + " +
                "dataModelUpdate.contents의 해당 키에 `{actionId, target, errorCode, message}` 형태로 포함된다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "A2UI 메시지 스트림 반환 (부분 실패 포함)"),
            ApiResponse(
                responseCode = "400",
                description = "요청 message 누락 또는 검증 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "LLM 호출 실패 등 서버 오류",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    fun chat(
        principal: Principal,
        @SwaggerRequestBody(
            description = "사용자 자연어 질문",
            required = true,
        )
        @Valid
        @RequestBody
        request: ChatRequest,
    ): ResponseEntity<DataResponseBody<ChatResponse>> =
        ResponseEntity.ok(DataResponseBody(chatService.chat(principal.name, request.message)))

    @PostMapping("/action")
    @Operation(
        summary = "페이지 이동 등 데이터만 재조회 (LLM 미사용)",
        description =
            "이미 생성된 화면에서 페이지네이션·필터 변경처럼 레이아웃 변경 없이 단일 액션 데이터만 다시 조회한다. " +
                "단건 호출 실패는 전역 예외 처리기를 통해 표준 ErrorResponseBody(HTTP 4xx/5xx)로 반환된다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "데이터 재조회 성공"),
            ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 (필터 형식 오류 등)",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "참조한 도메인 리소스(현장 등)를 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    fun action(
        @SwaggerRequestBody(
            description = "재조회 액션 요청 (actionId + target + filters)",
            required = true,
        )
        @Valid
        @RequestBody
        request: ChatActionRequest,
    ): ResponseEntity<DataResponseBody<ChatResponse>> = ResponseEntity.ok(DataResponseBody(chatService.handleAction(request)))
}
