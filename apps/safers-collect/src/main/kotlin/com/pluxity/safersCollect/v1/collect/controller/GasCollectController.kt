package com.pluxity.safersCollect.v1.collect.controller

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.GasEventAdapter
import com.pluxity.safersCollect.v1.collect.adapter.GasTelemetryAdapter
import com.pluxity.safersCollect.v1.collect.dto.GasEventRequest
import com.pluxity.safersCollect.v1.collect.dto.GasTelemetry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "1. 가스센서 수집", description = "유해가스 측정값 / 임계 초과 이벤트")
@RestController
@RequestMapping("/collect/gas")
class GasCollectController(
    private val gasAdapter: GasTelemetryAdapter,
    private val gasEventAdapter: GasEventAdapter,
    private val queue: ForwardQueue,
    private val eventForwarder: EventForwarder,
) {
    @Operation(summary = "유해가스 측정값 수집", description = "정상 범위 측정값 단건 시계열 적재용.")
    @PostMapping
    fun collect(
        @Valid @RequestBody request: GasTelemetry,
    ) {
        queue.enqueueAll(gasAdapter.toEnvelopes(request))
    }

    @Operation(summary = "유해가스 임계 초과 이벤트 수집", description = "임계 초과 시점에만 호출. 즉시 forward.")
    @PostMapping("/events")
    fun event(
        @Valid @RequestBody request: GasEventRequest,
    ) {
        eventForwarder.forward(gasEventAdapter.toEnvelope(request))
    }
}
