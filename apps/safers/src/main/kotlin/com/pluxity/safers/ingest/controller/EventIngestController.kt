package com.pluxity.safers.ingest.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.ingest.dto.EventIngestRequest
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
@Tag(name = "Ingest - Events", description = "수집모듈로부터 안전 사건 수신 (X-Api-Key 인증). siteId 는 URL path. 기존 /events 조회 API와는 다름")
class EventIngestController {
    @Operation(
        summary = "이벤트 적재",
        description = "수신 즉시 PostgreSQL 동기 INSERT 후 200 응답. payload는 JSONB로 저장.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "정상 접수"),
            ApiResponse(
                responseCode = "400",
                description = "스키마/검증 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "API Key 없음/무효",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PostMapping
    fun ingest(
        @PathVariable siteId: Long,
        @Valid @RequestBody request: EventIngestRequest,
    ): ResponseEntity<DataResponseBody<Unit>> {
        // TODO: PostgreSQL INSERT (site_id 컬럼은 path 값) + NOTIFY safety_event
        return ResponseEntity.ok(DataResponseBody(Unit))
    }
}
