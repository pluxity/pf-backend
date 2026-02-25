package com.pluxity.yongin.keymanagement.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.yongin.keymanagement.dto.KeyManagementGroupResponse
import com.pluxity.yongin.keymanagement.dto.KeyManagementRequest
import com.pluxity.yongin.keymanagement.dto.KeyManagementResponse
import com.pluxity.yongin.keymanagement.dto.KeyManagementTypeResponse
import com.pluxity.yongin.keymanagement.dto.KeyManagementUpdateRequest
import com.pluxity.yongin.keymanagement.dto.toResponse
import com.pluxity.yongin.keymanagement.entity.KeyManagementType
import com.pluxity.yongin.keymanagement.service.KeyManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/key-management")
@Tag(name = "Key Management Controller", description = "주요관리사항 API")
class KeyManagementController(
    private val service: KeyManagementService,
) {
    @Operation(summary = "주요관리사항 전체 조회", description = "주요관리사항 전체 목록을 타입별로 그룹화하여 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun findAll(): ResponseEntity<DataResponseBody<List<KeyManagementGroupResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findAll()))

    @Operation(summary = "대시보드용 선택된 주요관리사항 조회", description = "선택된 주요관리사항만 타입 순서 및 표시순서대로 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/selected")
    fun findSelected(): ResponseEntity<DataResponseBody<List<KeyManagementResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findSelected()))

    @Operation(summary = "주요관리사항 상세 조회", description = "주요관리사항을 상세 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "주요관리사항을 찾을 수 없음",
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
    fun findById(
        @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<KeyManagementResponse>> = ResponseEntity.ok(DataResponseBody(service.findById(id)))

    @Operation(summary = "주요관리사항 등록", description = "주요관리사항을 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공"),
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
    @ResponseCreated(path = "/key-management/{id}")
    fun create(
        @RequestBody @Valid request: KeyManagementRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(service.create(request))

    @Operation(summary = "주요관리사항 수정", description = "주요관리사항을 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "수정 성공"),
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
            ApiResponse(
                responseCode = "404",
                description = "주요관리사항을 찾을 수 없음",
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
    fun update(
        @PathVariable id: Long,
        @RequestBody @Valid request: KeyManagementUpdateRequest,
    ): ResponseEntity<Void> {
        service.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "주요관리사항 삭제", description = "주요관리사항을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "주요관리사항을 찾을 수 없음",
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
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "주요관리사항 타입 목록 조회", description = "주요관리사항 타입 enum 목록을 조회합니다")
    @GetMapping("/types")
    fun getTypes(): ResponseEntity<DataResponseBody<List<KeyManagementTypeResponse>>> =
        ResponseEntity.ok(DataResponseBody(KeyManagementType.entries.map { it.toResponse() }))

    @Operation(summary = "주요관리사항 선택", description = "주요관리사항을 대시보드에 표시되도록 선택합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "선택 성공"),
            ApiResponse(
                responseCode = "404",
                description = "주요관리사항을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/{id}/select")
    fun select(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.select(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "주요관리사항 선택 해제", description = "주요관리사항의 대시보드 표시를 해제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "선택 해제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "주요관리사항을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/{id}/deselect")
    fun deselect(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.deselect(id)
        return ResponseEntity.noContent().build()
    }
}
