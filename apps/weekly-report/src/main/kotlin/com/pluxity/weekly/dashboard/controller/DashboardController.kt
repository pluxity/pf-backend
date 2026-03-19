package com.pluxity.weekly.dashboard.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.weekly.dashboard.dto.WorkerDashboardResponse
import com.pluxity.weekly.dashboard.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
}
