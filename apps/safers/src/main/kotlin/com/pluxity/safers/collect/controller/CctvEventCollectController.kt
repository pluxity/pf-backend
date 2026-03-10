package com.pluxity.safers.collect.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventVideoUploadRequest
import com.pluxity.safers.event.service.EventFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Event Collect Controller", description = "이벤트 수집 API")
class CctvEventCollectController(
    private val eventFacade: EventFacade,
    private val cctvEventCollector: CctvEventCollector,
) {
    @Operation(summary = "이벤트 등록", description = "외부 시스템에서 감지된 이벤트를 수집하여 등록합니다")
    @ResponseCreated(path = "/events/{id}")
    @PostMapping
    fun collectEvent(
        @Parameter(description = "이벤트 등록 정보", required = true)
        @RequestBody
        @Valid
        request: EventCreateRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(eventFacade.create(request))

    @Operation(summary = "이벤트 영상 등록", description = "이벤트에 대한 영상을 수집하여 Kafka를 통해 등록합니다")
    @PostMapping("/{eventId}/video")
    fun collectVideo(
        @PathVariable
        @Parameter(description = "이벤트 ID", required = true)
        eventId: Long,
        @RequestBody
        @Valid
        request: EventVideoUploadRequest,
    ): ResponseEntity<Void> {
        cctvEventCollector.collectVideo(eventId, request)
        return ResponseEntity.accepted().build()
    }
}
