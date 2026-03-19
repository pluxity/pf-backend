package com.pluxity.weekly.dashboard.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.weekly.dashboard.dto.PmDashboardResponse
import com.pluxity.weekly.dashboard.dto.WorkerDashboardResponse
import com.pluxity.weekly.dashboard.service.DashboardService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard Controller", description = "대시보드 API")
class DashboardController(
    private val service: DashboardService,
) {
    @Operation(summary = "작업자 대시보드 조회", description = "로그인한 사용자의 대시보드 데이터를 조회합니다")
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
    @GetMapping("/worker")
    fun getWorkerDashboard(): ResponseEntity<DataResponseBody<WorkerDashboardResponse>> =
        ResponseEntity.ok(DataResponseBody(service.getWorkerDashboard()))

    @Operation(summary = "PM 대시보드 조회", description = "PM 권한으로 프로젝트 대시보드를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "프로젝트 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/pm/{projectId}")
    fun getPmDashboard(
        @Parameter(description = "프로젝트 ID", example = "1")
        @PathVariable projectId: Long,
    ): ResponseEntity<DataResponseBody<PmDashboardResponse>> =
        ResponseEntity.ok(DataResponseBody(service.getPmDashboard(projectId)))
}
