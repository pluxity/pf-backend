package com.pluxity.safers.site.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.site.dto.RegionResponse
import com.pluxity.safers.site.dto.SiteRequest
import com.pluxity.safers.site.dto.SiteResponse
import com.pluxity.safers.site.dto.toResponse
import com.pluxity.safers.site.entity.Region
import com.pluxity.safers.site.service.SiteService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sites")
@Tag(name = "Site Controller", description = "현장 관리 API")
class SiteController(
    private val siteService: SiteService,
) {
    @Operation(summary = "현장 등록", description = "새로운 현장을 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "현장 등록 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PostMapping
    @ResponseCreated(path = "/sites/{id}")
    fun createSite(
        @Parameter(description = "현장 등록 정보", required = true)
        @RequestBody
        @Valid
        request: SiteRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(siteService.create(request))

    @Operation(summary = "현장 목록 조회", description = "모든 현장 목록을 페이지네이션으로 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        ],
    )
    @GetMapping
    fun getAllSites(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page")
        page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "9999")
        @RequestParam("size")
        size: Int = 9999,
    ): ResponseEntity<DataResponseBody<PageResponse<SiteResponse>>> =
        ResponseEntity.ok(DataResponseBody(siteService.findAll(PageSearchRequest(page, size))))

    @Operation(summary = "현장 상세 조회", description = "ID로 특정 현장의 상세 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "현장 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "현장을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun getSite(
        @PathVariable
        @Parameter(description = "현장 ID", required = true)
        id: Long,
    ): ResponseEntity<DataResponseBody<SiteResponse>> = ResponseEntity.ok(DataResponseBody(siteService.findById(id)))

    @Operation(summary = "현장 수정", description = "현장 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "현장 수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "현장을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping("/{id}")
    fun updateSite(
        @PathVariable
        @Parameter(description = "현장 ID", required = true)
        id: Long,
        @Parameter(description = "현장 수정 정보", required = true)
        @RequestBody
        @Valid
        request: SiteRequest,
    ): ResponseEntity<Void> {
        siteService.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "현장 삭제", description = "현장을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "현장 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "현장을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteSite(
        @PathVariable
        @Parameter(description = "현장 ID", required = true)
        id: Long,
    ): ResponseEntity<Void> {
        siteService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "지역 목록 조회", description = "지역 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        ],
    )
    @GetMapping("/regions")
    fun getRegions(): ResponseEntity<DataResponseBody<List<RegionResponse>>> =
        ResponseEntity.ok(DataResponseBody(Region.entries.map { it.toResponse() }))
}
