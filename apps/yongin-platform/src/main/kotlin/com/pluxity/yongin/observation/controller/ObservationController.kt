package com.pluxity.yongin.observation.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.yongin.observation.dto.ObservationRequest
import com.pluxity.yongin.observation.dto.ObservationResponse
import com.pluxity.yongin.observation.service.ObservationService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/observations")
@Tag(name = "Observation Controller", description = "드론 관측 데이터 API")
class ObservationController(
    private val service: ObservationService,
) {
    @Operation(summary = "드론 관측 데이터 목록 조회", description = "모든 드론 관측 데이터를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    @GetMapping
    fun findAll(): ResponseEntity<DataResponseBody<List<ObservationResponse>>> = ResponseEntity.ok(DataResponseBody(service.findAll()))

    @Operation(summary = "드론 관측 데이터 단건 조회", description = "ID로 드론 관측 데이터를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "데이터를 찾을 수 없음",
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
    ): ResponseEntity<DataResponseBody<ObservationResponse>> = ResponseEntity.ok(DataResponseBody(service.findById(id)))

    @Operation(summary = "드론 관측 데이터 등록", description = "드론 관측 데이터를 등록합니다")
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
    @ResponseCreated(path = "/observations/{id}")
    fun create(
        @RequestBody @Valid request: ObservationRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(service.create(request))

    @Operation(summary = "드론 관측 데이터 수정", description = "드론 관측 데이터를 수정합니다")
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
                description = "데이터를 찾을 수 없음",
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
        @RequestBody @Valid request: ObservationRequest,
    ): ResponseEntity<Void> {
        service.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "드론 관측 데이터 삭제", description = "드론 관측 데이터를 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "데이터를 찾을 수 없음",
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
}
