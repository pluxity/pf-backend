package com.pluxity.yongin.processstatus.controller

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.processstatus.dto.ProcessStatusBulkRequest
import com.pluxity.yongin.processstatus.dto.ProcessStatusImageRequest
import com.pluxity.yongin.processstatus.dto.ProcessStatusImageResponse
import com.pluxity.yongin.processstatus.dto.ProcessStatusResponse
import com.pluxity.yongin.processstatus.service.ProcessStatusImageService
import com.pluxity.yongin.processstatus.service.ProcessStatusService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/process-statuses")
@Tag(name = "Process Status Controller", description = "공정현황 관리 API")
class ProcessStatusController(
    private val service: ProcessStatusService,
    private val imageService: ProcessStatusImageService,
) {
    @Operation(summary = "공정현황 전체 조회", description = "공정현황 전체 목록을 조회합니다")
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
    fun findAll(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page") page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "9999")
        @RequestParam("size") size: Int = 9999,
    ): ResponseEntity<DataResponseBody<PageResponse<ProcessStatusResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findAll(PageSearchRequest(page, size))))

    @Operation(summary = "최근 공정현황 조회", description = "가장 최근 작업일자의 공정현황 목록을 조회합니다")
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
    @GetMapping("/latest")
    fun findLatest(): ResponseEntity<DataResponseBody<List<ProcessStatusResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findLatest()))

    @Operation(
        summary = "공정현황 저장/수정/삭제",
        description = "공정현황을 저장, 수정, 삭제합니다. upserts의 id가 없으면 생성, 있으면 수정합니다. deletedIds에 포함된 id는 삭제됩니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "송출 성공"),
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
    @PutMapping
    fun saveOrUpdateAll(
        @RequestBody @Valid request: ProcessStatusBulkRequest,
    ): ResponseEntity<Void> {
        service.saveOrUpdateAll(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "공정관련 이미지 조회", description = "공정관련 이미지를 조회합니다")
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
    @GetMapping("/image")
    fun findImage(): ResponseEntity<DataResponseBody<ProcessStatusImageResponse>> =
        ResponseEntity.ok(DataResponseBody(imageService.getImage()))

    @Operation(summary = "공정관련 이미지 수정", description = "공정관련 이미지를 수정합니다 (없으면 생성)")
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
        ],
    )
    @PutMapping("/image")
    fun saveImage(
        @RequestBody @Valid request: ProcessStatusImageRequest,
    ): ResponseEntity<Void> {
        imageService.saveImage(request)
        return ResponseEntity.noContent().build()
    }
}
