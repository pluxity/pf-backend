package com.pluxity.yonginplatform.systemsetting.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.yonginplatform.systemsetting.dto.SystemSettingRequest
import com.pluxity.yonginplatform.systemsetting.dto.SystemSettingResponse
import com.pluxity.yonginplatform.systemsetting.service.SystemSettingService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/system-settings")
@Tag(name = "SystemSetting Controller", description = "시스템 설정 관리 API")
class SystemSettingController(
    private val systemSettingService: SystemSettingService,
) {
    @Operation(summary = "시스템 설정 조회", description = "시스템 설정을 조회합니다")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "시스템 설정 조회 성공")])
    @GetMapping
    fun getSystemSetting(): ResponseEntity<DataResponseBody<SystemSettingResponse>> =
        ResponseEntity.ok(DataResponseBody(systemSettingService.find()))

    @Operation(summary = "시스템 설정 수정", description = "시스템 설정을 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "시스템 설정 수정 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping
    fun updateSystemSetting(
        @Parameter(description = "시스템 설정 수정 정보", required = true) @RequestBody @Valid request: SystemSettingRequest,
    ): ResponseEntity<Void> {
        systemSettingService.update(request)
        return ResponseEntity.noContent().build()
    }
}
