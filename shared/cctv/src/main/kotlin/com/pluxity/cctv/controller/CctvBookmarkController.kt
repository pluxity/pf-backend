package com.pluxity.cctv.controller

import com.pluxity.cctv.dto.CctvBookmarkOrderRequest
import com.pluxity.cctv.dto.CctvBookmarkRequest
import com.pluxity.cctv.dto.CctvBookmarkResponse
import com.pluxity.cctv.service.CctvBookmarkService
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctv-bookmarks")
@Tag(name = "CCTV Bookmark Controller", description = "CCTV 즐겨찾기 관리 API")
class CctvBookmarkController(
    private val service: CctvBookmarkService,
) {
    @Operation(summary = "CCTV 즐겨찾기 목록 조회", description = "즐겨찾기 등록된 CCTV 목록을 표시 순서대로 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    @GetMapping
    fun findAll(): ResponseEntity<DataResponseBody<List<CctvBookmarkResponse>>> = ResponseEntity.ok(DataResponseBody(service.findAll()))

    @Operation(summary = "CCTV 즐겨찾기 등록", description = "CCTV를 즐겨찾기에 등록합니다 (최대 개수는 설정에 따름)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 성공"),
            ApiResponse(
                responseCode = "400",
                description = "즐겨찾기 최대 개수 초과 또는 이미 즐겨찾기된 CCTV",
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
    fun create(
        @RequestBody @Valid request: CctvBookmarkRequest,
    ): ResponseEntity<DataResponseBody<Long>> = ResponseEntity.ok(DataResponseBody(service.create(request)))

    @Operation(summary = "CCTV 즐겨찾기 삭제", description = "CCTV 즐겨찾기를 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "즐겨찾기를 찾을 수 없음",
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

    @Operation(summary = "CCTV 즐겨찾기 순서 변경", description = "즐겨찾기의 표시 순서를 변경합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "순서 변경 성공"),
            ApiResponse(
                responseCode = "404",
                description = "즐겨찾기를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/order")
    fun updateOrder(
        @RequestBody @Valid request: CctvBookmarkOrderRequest,
    ): ResponseEntity<Void> {
        service.updateOrder(request)
        return ResponseEntity.noContent().build()
    }
}
