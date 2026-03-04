package com.pluxity.yongin.attendance.controller

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.attendance.dto.AttendanceResponse
import com.pluxity.yongin.attendance.dto.AttendanceUpdateRequest
import com.pluxity.yongin.attendance.service.AttendanceFacade
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/attendances")
@Tag(name = "Attendance Controller", description = "출역현황 관리 API")
class AttendanceController(
    private val service: AttendanceFacade,
) {
    @Operation(summary = "출역현황 전체 조회", description = "외부 API를 호출하여 데이터를 동기화한 후 출역현황을 조회합니다")
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
    ): ResponseEntity<DataResponseBody<PageResponse<AttendanceResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findAllWithSync(PageSearchRequest(page, size))))

    @Operation(summary = "최근 출역현황 조회", description = "가장 최근 날짜의 출역현황 목록을 조회합니다")
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
    fun findLatest(): ResponseEntity<DataResponseBody<List<AttendanceResponse>>> = ResponseEntity.ok(DataResponseBody(service.findLatest()))

    @Operation(summary = "출역현황 작업내용 수정", description = "출역현황의 작업내용을 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "출역현황을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateWorkContent(
        @PathVariable id: Long,
        @RequestBody @Valid request: AttendanceUpdateRequest,
    ): ResponseEntity<Void> {
        service.updateWorkContent(id, request)
        return ResponseEntity.noContent().build()
    }
}
