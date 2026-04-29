package com.pluxity.safers.ingest.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.ingest.dto.TelemetryRequest
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
@RequestMapping("/v1/sites/{siteId}/telemetry")
@Tag(name = "Ingest - Telemetry", description = "수집모듈로부터 정규화된 시계열 측정값 수신 (X-Api-Key 인증). siteId 는 URL path.")
class TelemetryController {
    @Operation(
        summary = "telemetry 적재",
        description = "정규화 envelope을 받아 InfluxDB 배치 큐에 enqueue 후 즉시 200 응답.",
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
        @Valid @RequestBody request: TelemetryRequest,
    ): ResponseEntity<DataResponseBody<Unit>> {
        // TODO: InfluxDB writer enqueue (siteId 를 measurement tag 로 부착)
        return ResponseEntity.ok(DataResponseBody(Unit))
    }
}
