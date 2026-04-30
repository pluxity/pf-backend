package com.pluxity.safers.ingest.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.ingest.dto.DeviceCreateRequest
import com.pluxity.safers.ingest.dto.DeviceResponse
import com.pluxity.safers.ingest.dto.DeviceUpdateRequest
import com.pluxity.safers.ingest.enums.DeviceStatus
import com.pluxity.safers.ingest.enums.DeviceType
import com.pluxity.safers.ingest.service.DeviceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sites/{siteId}/devices")
@Tag(name = "Ingest - Devices", description = "통합 디바이스 마스터 CRUD — type 디스크리미네이터 + metadata jsonb. (id, siteId) 가 유일 키")
class DeviceController(
    private val deviceService: DeviceService,
) {
    @Operation(summary = "디바이스 등록", description = "(id, siteId) 가 이미 존재하면 409.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공"),
            ApiResponse(
                responseCode = "400",
                description = "검증 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "(id, siteId) 중복",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PostMapping
    fun create(
        @PathVariable siteId: Long,
        @Valid @RequestBody request: DeviceCreateRequest,
    ): ResponseEntity<DataResponseBody<DeviceResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(DataResponseBody(deviceService.create(siteId, request)))

    @Operation(summary = "디바이스 목록 조회", description = "type / status 필터 지원. siteId 는 path.")
    @GetMapping
    fun list(
        @PathVariable siteId: Long,
        @Parameter(description = "디바이스 종류 필터") @RequestParam(required = false) type: DeviceType?,
        @Parameter(description = "상태 필터") @RequestParam(required = false) status: DeviceStatus?,
    ): ResponseEntity<DataResponseBody<List<DeviceResponse>>> =
        ResponseEntity.ok(DataResponseBody(deviceService.list(siteId, type, status)))

    @Operation(summary = "디바이스 단건 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "디바이스 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun get(
        @PathVariable siteId: Long,
        @PathVariable id: String,
    ): ResponseEntity<DataResponseBody<DeviceResponse>> = ResponseEntity.ok(DataResponseBody(deviceService.get(id, siteId)))

    @Operation(summary = "디바이스 부분 수정")
    @PatchMapping("/{id}")
    fun update(
        @PathVariable siteId: Long,
        @PathVariable id: String,
        @Valid @RequestBody request: DeviceUpdateRequest,
    ): ResponseEntity<DataResponseBody<DeviceResponse>> = ResponseEntity.ok(DataResponseBody(deviceService.update(id, siteId, request)))

    @Operation(summary = "디바이스 폐기/삭제")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "디바이스 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable siteId: Long,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        deviceService.delete(id, siteId)
        return ResponseEntity.noContent().build()
    }
}
