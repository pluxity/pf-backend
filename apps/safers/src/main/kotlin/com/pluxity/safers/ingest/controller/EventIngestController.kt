package com.pluxity.safers.ingest.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.ingest.dto.EventIngestRequest
import com.pluxity.safers.ingest.dto.SafetyEventResponse
import com.pluxity.safers.ingest.service.SafetyEventIngestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sites/{siteId}/events")
@Tag(name = "Ingest - Safety Events", description = "수집모듈로부터 안전 사건(가스/SOS/밴드) 수신. siteId 는 URL path. 기존 /events 조회 API 와는 다름")
class EventIngestController(
    private val safetyEventIngestService: SafetyEventIngestService,
) {
    @Operation(
        summary = "안전 이벤트 적재",
        description = "수신 즉시 PostgreSQL events 테이블에 category=SAFETY 로 INSERT 하고, STOMP /topic/safety-events 로 브로드캐스트.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "정상 접수"),
            ApiResponse(
                responseCode = "400",
                description = "스키마/검증 실패 또는 EventType 이 SAFETY 카테고리가 아님",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "동일 eventId 중복",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PostMapping
    fun ingest(
        @PathVariable siteId: Long,
        @Valid @RequestBody request: EventIngestRequest,
    ): ResponseEntity<DataResponseBody<SafetyEventResponse>> =
        ResponseEntity.ok(DataResponseBody(safetyEventIngestService.ingest(siteId, request)))
}
