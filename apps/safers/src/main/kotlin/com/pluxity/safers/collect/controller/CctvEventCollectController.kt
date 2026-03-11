package com.pluxity.safers.collect.controller

import com.pluxity.safers.collect.dto.CctvVideoCollectRequest
import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.event.dto.EventCreateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/collect/cctv/events")
@Tag(name = "CCTV 이벤트 수집", description = "CCTV 이벤트 수집 API (Kafka 비동기)")
class CctvEventCollectController(
    private val cctvEventCollector: CctvEventCollector,
) {
    @Operation(summary = "이벤트 수집", description = "외부 시스템에서 감지된 이벤트를 수집하여 Kafka를 통해 비동기 등록합니다")
    @PostMapping
    fun collectEvent(
        @Parameter(description = "이벤트 등록 정보", required = true)
        @RequestBody
        @Valid
        request: EventCreateRequest,
    ): ResponseEntity<Void> {
        cctvEventCollector.collect(request)
        return ResponseEntity.accepted().build()
    }

    @Operation(summary = "이벤트 영상 수집", description = "이벤트에 대한 영상을 수집하여 Kafka를 통해 비동기 등록합니다")
    @PostMapping("/video")
    fun collectVideo(
        @Parameter(description = "영상 수집 정보", required = true)
        @RequestBody
        @Valid
        request: CctvVideoCollectRequest,
    ): ResponseEntity<Void> {
        cctvEventCollector.collectVideo(request)
        return ResponseEntity.accepted().build()
    }
}
