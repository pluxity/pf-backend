package com.pluxity.safers.event.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventCategoryResponse
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.dto.EventVideoUploadRequest
import com.pluxity.safers.event.dto.toResponse
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.service.EventService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Event Controller", description = "이벤트 관리 API")
class EventController(
    private val eventService: EventService,
) {
    @Operation(summary = "이벤트 등록", description = "외부 시스템에서 감지된 이벤트를 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "이벤트 등록 성공",
            ),
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
    @PostMapping
    @ResponseCreated(path = "/events/{id}")
    fun createEvent(
        @Parameter(description = "이벤트 등록 정보", required = true)
        @RequestBody
        @Valid
        request: EventCreateRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(eventService.create(request))

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
    ): ResponseEntity<DataResponseBody<PageResponse<EventResponse>>> =
        ResponseEntity.ok(DataResponseBody(eventService.findAll(PageSearchRequest(page, size))))

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
    ): ResponseEntity<DataResponseBody<EventResponse>> = ResponseEntity.ok(DataResponseBody(eventService.findById(id)))

    @Operation(summary = "이벤트 영상 등록", description = "외부 시스템의 영상 파일을 다운로드하여 이벤트에 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "영상 등록 성공",
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
    @PostMapping("/{eventId}/video")
    fun uploadVideo(
        @PathVariable
        @Parameter(description = "이벤트 ID", required = true)
        eventId: Long,
        @RequestBody
        @Valid
        request: EventVideoUploadRequest,
    ): ResponseEntity<Void> {
        eventService.uploadVideo(eventId, request.video)
        return ResponseEntity.ok().build()
    }

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
