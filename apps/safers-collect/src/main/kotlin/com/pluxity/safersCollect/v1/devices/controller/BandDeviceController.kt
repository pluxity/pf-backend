package com.pluxity.safersCollect.v1.devices.controller

import com.pluxity.safersCollect.v1.devices.dto.BandDeviceCreateRequest
import com.pluxity.safersCollect.v1.devices.dto.BandDeviceResponse
import com.pluxity.safersCollect.v1.devices.dto.BandDeviceUpdateRequest
import com.pluxity.safersCollect.v1.devices.dto.DeviceStatus
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

@Tag(name = "5. 스마트밴드 디바이스 CRUD", description = "현장 등록/수정/삭제 — 중앙 /v1/devices 로 forward")
@RestController
@RequestMapping("/devices/bands")
class BandDeviceController {
    @Operation(summary = "스마트밴드 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: BandDeviceCreateRequest,
    ): BandDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "스마트밴드 목록 (현 사이트)")
    @GetMapping
    fun list(
        @Parameter(description = "상태 필터") @RequestParam(required = false) status: DeviceStatus?,
    ): List<BandDeviceResponse> {
        // TODO
        return emptyList()
    }

    @Operation(summary = "스마트밴드 단건 조회")
    @GetMapping("/{bandId}")
    fun get(
        @PathVariable bandId: String,
    ): BandDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "스마트밴드 부분 수정 (워커 재할당 등)")
    @PatchMapping("/{bandId}")
    fun update(
        @PathVariable bandId: String,
        @Valid @RequestBody request: BandDeviceUpdateRequest,
    ): BandDeviceResponse {
        // TODO
        TODO("not implemented")
    }

    @Operation(summary = "스마트밴드 폐기/삭제")
    @DeleteMapping("/{bandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable bandId: String,
    ) {
        // TODO
    }
}
