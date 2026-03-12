package com.pluxity.safers.event.controller

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventCategoryResponse
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.dto.toResponse
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.service.EventFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Event Controller", description = "이벤트 관리 API")
class EventController(
    private val eventFacade: EventFacade,
) {
    @Operation(summary = "이벤트 목록 조회", description = "모든 이벤트 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
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
    @GetMapping
    fun getAllEvents(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page")
        page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "9999")
        @RequestParam("size")
        size: Int = 9999,
        @Parameter(description = "시작일시 (yyyyMMddHHmmss)", example = "20260101000000")
        @RequestParam(required = false)
        startDate: String? = null,
        @Parameter(description = "종료일시 (yyyyMMddHHmmss)", example = "20261231235959")
        @RequestParam(required = false)
        endDate: String? = null,
    ): ResponseEntity<DataResponseBody<PageResponse<EventResponse>>> =
        ResponseEntity.ok(DataResponseBody(eventFacade.findAll(PageSearchRequest(page, size), startDate, endDate)))

    @Operation(summary = "이벤트 상세 조회", description = "ID로 특정 이벤트의 상세 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이벤트 조회 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "이벤트를 찾을 수 없음",
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
    @GetMapping("/{id}")
    fun getEvent(
        @PathVariable
        @Parameter(description = "이벤트 ID", required = true)
        id: Long,
    ): ResponseEntity<DataResponseBody<EventResponse>> = ResponseEntity.ok(DataResponseBody(eventFacade.findById(id)))

    @Operation(summary = "이벤트 카테고리 목록 조회", description = "이벤트 카테고리 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
            ),
        ],
    )
    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<DataResponseBody<List<EventCategoryResponse>>> =
        ResponseEntity.ok(DataResponseBody(EventCategory.entries.map { it.toResponse() }))
}
