package com.pluxity.safers.weather.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.safers.weather.dto.WeatherTimeGroupResponse
import com.pluxity.safers.weather.service.WeatherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sites/{siteId}/weather")
@Tag(name = "Weather Controller", description = "날씨 정보 API")
class WeatherController(
    private val weatherService: WeatherService,
) {
    @Operation(summary = "날씨 대시보드 조회", description = "현재 시간 기준 전 3시간 ~ 후 5시간 범위의 날씨 데이터를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    @GetMapping
    fun getDashboard(
        @PathVariable
        @Parameter(description = "현장 ID", required = true)
        siteId: Long,
    ): ResponseEntity<DataResponseBody<List<WeatherTimeGroupResponse>>> =
        ResponseEntity.ok(DataResponseBody(weatherService.findDashboard(siteId)))
}
