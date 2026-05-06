package com.pluxity.safersCollect.v1.collect.controller

import com.pluxity.safersCollect.buffer.TelemetryBuffer
import com.pluxity.safersCollect.v1.collect.adapter.GasTelemetryAdapter
import com.pluxity.safersCollect.v1.collect.dto.GasCollectRequest
import com.pluxity.safersCollect.v1.collect.dto.GasEventRequest
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
    private val buffer: TelemetryBuffer,
) {
    @Operation(summary = "유해가스 측정값 수집", description = "정상 범위 측정값 시계열 적재용. 1~500건 batch.")
    @PostMapping
    fun collect(
        @Valid @RequestBody request: GasCollectRequest,
    ) {
        buffer.enqueueAll(gasAdapter.toEnvelopes(request))
    }

    @Operation(summary = "유해가스 임계 초과 이벤트 수집", description = "임계 초과 시점에만 호출. 동기 처리.")
    @PostMapping("/events")
    fun event(
        @Valid @RequestBody request: GasEventRequest,
    ) {
        // TODO: 어댑터 → 정규화 envelope → buffer (immediate forward 분기 대상)
    }
}
