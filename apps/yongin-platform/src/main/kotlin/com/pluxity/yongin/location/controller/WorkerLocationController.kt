package com.pluxity.yongin.location.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.yongin.location.dto.WorkerLocationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/worker-locations")
@Tag(name = "Worker Location Controller", description = "근로자 위치 정보 API")
class WorkerLocationController {
    @Operation(summary = "전체 근로자 위치 조회", description = "모든 근로자의 현재 위치 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    @GetMapping
    fun getAll(): ResponseEntity<DataResponseBody<List<WorkerLocationResponse>>> =
        ResponseEntity.ok(DataResponseBody(mockWorkerLocations()))

    @Operation(summary = "근로자 위치 조회", description = "특정 근로자의 현재 위치 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    @GetMapping("/{workerId}")
    fun getByWorkerId(
        @PathVariable workerId: Long,
    ): ResponseEntity<DataResponseBody<WorkerLocationResponse>> = ResponseEntity.ok(DataResponseBody(mockWorkerLocation(workerId)))

    private fun mockWorkerLocations(): List<WorkerLocationResponse> {
        val now = LocalDateTime.now()
        return listOf(
            WorkerLocationResponse(workerId = 1L, latitude = 37.2411, longitude = 127.1775, timestamp = now, accuracy = 3.5),
            WorkerLocationResponse(workerId = 2L, latitude = 37.2425, longitude = 127.1790, timestamp = now, accuracy = 2.1),
            WorkerLocationResponse(workerId = 3L, latitude = 37.2398, longitude = 127.1762, timestamp = now, accuracy = 5.0),
        )
    }

    private fun mockWorkerLocation(workerId: Long): WorkerLocationResponse =
        WorkerLocationResponse(
            workerId = workerId,
            latitude = 37.2411,
            longitude = 127.1775,
            timestamp = LocalDateTime.now(),
            accuracy = 3.5,
        )
}
