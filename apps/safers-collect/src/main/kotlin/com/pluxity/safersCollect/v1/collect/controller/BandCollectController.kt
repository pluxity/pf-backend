package com.pluxity.safersCollect.v1.collect.controller

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.BandEventAdapter
import com.pluxity.safersCollect.v1.collect.adapter.BandTelemetryAdapter
import com.pluxity.safersCollect.v1.collect.dto.BandCollectRequest
import com.pluxity.safersCollect.v1.collect.dto.BandEventRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "2. 스마트밴드 수집", description = "근로자 웨어러블 측정값 / 이상 이벤트")
@RestController
@RequestMapping("/collect/band")
class BandCollectController(
    private val adapter: BandTelemetryAdapter,
    private val bandEventAdapter: BandEventAdapter,
    private val queue: ForwardQueue,
    private val eventForwarder: EventForwarder,
) {
    @Operation(summary = "스마트밴드 측정값 수집", description = "위치/체온/심박수/SpO2 등 단건.")
    @PostMapping
    fun collect(
        @Valid @RequestBody request: BandCollectRequest,
    ) {
        queue.enqueueAll(adapter.toEnvelopes(request))
    }

    @Operation(
        summary = "스마트밴드 이상 이벤트 수집",
        description = "VITAL_ABNORMAL / FALL_DETECTED / OFFLINE 을 eventType 으로 구분해 단일 엔드포인트에서 처리. 즉시 forward.",
    )
    @PostMapping("/events")
    fun event(
        @Valid @RequestBody request: BandEventRequest,
    ) {
        eventForwarder.forward(bandEventAdapter.toEnvelope(request))
    }
}
