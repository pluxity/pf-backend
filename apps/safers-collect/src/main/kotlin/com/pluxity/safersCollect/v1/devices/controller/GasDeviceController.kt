package com.pluxity.safersCollect.v1.devices.controller

import com.pluxity.safersCollect.v1.devices.dto.DeviceStatus
import com.pluxity.safersCollect.v1.devices.dto.GasDeviceCreateRequest
import com.pluxity.safersCollect.v1.devices.dto.GasDeviceResponse
import com.pluxity.safersCollect.v1.devices.dto.GasDeviceUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "4. 가스센서 디바이스 CRUD", description = "현장 등록/수정/삭제 — 중앙 /v1/devices 로 forward")
@RestController
@RequestMapping("/devices/gas")
class GasDeviceController {
    @Operation(summary = "가스센서 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: GasDeviceCreateRequest,
    ): GasDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "가스센서 목록 (현 사이트)")
    @GetMapping
    fun list(
        @Parameter(description = "상태 필터") @RequestParam(required = false) status: DeviceStatus?,
    ): List<GasDeviceResponse> {
        // TODO
        return emptyList()
    }

    @Operation(summary = "가스센서 단건 조회")
    @GetMapping("/{deviceId}")
    fun get(
        @PathVariable deviceId: String,
    ): GasDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "가스센서 부분 수정")
    @PatchMapping("/{deviceId}")
    fun update(
        @PathVariable deviceId: String,
        @Valid @RequestBody request: GasDeviceUpdateRequest,
    ): GasDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "가스센서 폐기/삭제")
    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable deviceId: String,
    ) {
        // TODO
    }
}
