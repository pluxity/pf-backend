package com.pluxity.yonginplatform.safetyequipment.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.yonginplatform.safetyequipment.dto.SafetyEquipmentRequest
import com.pluxity.yonginplatform.safetyequipment.dto.SafetyEquipmentResponse
import com.pluxity.yonginplatform.safetyequipment.service.SafetyEquipmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/safety-equipments")
@Tag(name = "SafetyEquipment Controller", description = "안전장비 관리 API")
class SafetyEquipmentController(
    private val safetyEquipmentService: SafetyEquipmentService,
) {
    @Operation(summary = "안전장비 등록", description = "새로운 안전장비를 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "안전장비 등록 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    @ResponseCreated(path = "/safety-equipments/{id}")
    fun createSafetyEquipment(
        @Parameter(description = "안전장비 등록 정보", required = true) @RequestBody @Valid request: SafetyEquipmentRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(safetyEquipmentService.create(request))

    @Operation(summary = "안전장비 목록 조회", description = "모든 안전장비 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        ],
    )
    @GetMapping
    fun getAllSafetyEquipments(): ResponseEntity<DataResponseBody<List<SafetyEquipmentResponse>>> =
        ResponseEntity.ok(DataResponseBody(safetyEquipmentService.findAll()))

    @Operation(summary = "안전장비 상세 조회", description = "ID로 특정 안전장비의 상세 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "안전장비 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "안전장비를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun getSafetyEquipment(
        @PathVariable @Parameter(description = "안전장비 ID", required = true) id: Long,
    ): ResponseEntity<DataResponseBody<SafetyEquipmentResponse>> = ResponseEntity.ok(DataResponseBody(safetyEquipmentService.findById(id)))

    @Operation(summary = "안전장비 수정", description = "안전장비 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "안전장비 수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "안전장비를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PutMapping("/{id}")
    fun updateSafetyEquipment(
        @PathVariable @Parameter(description = "안전장비 ID", required = true) id: Long,
        @Parameter(description = "안전장비 수정 정보", required = true) @RequestBody @Valid request: SafetyEquipmentRequest,
    ): ResponseEntity<Void> {
        safetyEquipmentService.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "안전장비 삭제", description = "안전장비를 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "안전장비 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "안전장비를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteSafetyEquipment(
        @PathVariable @Parameter(description = "안전장비 ID", required = true) id: Long,
    ): ResponseEntity<Void> {
        safetyEquipmentService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
