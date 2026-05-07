package com.pluxity.safersCollect.v1.collect.controller

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.v1.collect.adapter.SosEventAdapter
import com.pluxity.safersCollect.v1.collect.dto.SosEventRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "3. SOS 수집", description = "근로자 SOS 긴급호출")
@RestController
@RequestMapping("/collect/sos")
class SosEventController(
    private val sosEventAdapter: SosEventAdapter,
    private val eventForwarder: EventForwarder,
) {
    @Operation(
        summary = "SOS 이벤트 수집",
        description = "BUTTON_LONG_PRESS / MOBILE_APP / MANUAL_DISPATCH 모두 본 엔드포인트로 수신. 즉시 forward.",
    )
    @PostMapping("/events")
    fun event(
        @Valid @RequestBody request: SosEventRequest,
    ) {
        eventForwarder.forward(sosEventAdapter.toEnvelope(request))
    }
}
