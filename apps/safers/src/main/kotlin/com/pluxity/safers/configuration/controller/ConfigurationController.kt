package com.pluxity.safers.configuration.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.configuration.dto.ConfigurationRequest
import com.pluxity.safers.configuration.dto.ConfigurationResponse
import com.pluxity.safers.configuration.dto.ConfigurationUpdateRequest
import com.pluxity.safers.configuration.service.ConfigurationService
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
@RequestMapping("/configurations")
@Tag(name = "Configuration Controller", description = "설정 관리 API")
class ConfigurationController(
    private val configurationService: ConfigurationService,
) {
    @Operation(summary = "설정 등록", description = "새로운 설정을 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "설정 등록 성공"),
            ApiResponse(
                responseCode = "409",
                description = "설정 키 중복",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PostMapping
    @ResponseCreated(path = "/configurations/{id}")
    fun createConfiguration(
        @Parameter(description = "설정 등록 정보", required = true)
        @RequestBody
        @Valid
        request: ConfigurationRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(configurationService.create(request))

    @Operation(summary = "설정 목록 조회", description = "모든 설정 목록을 페이지네이션으로 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        ],
    )
    @GetMapping
    fun getAllConfigurations(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page")
        page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "9999")
        @RequestParam("size")
        size: Int = 9999,
    ): ResponseEntity<DataResponseBody<PageResponse<ConfigurationResponse>>> =
        ResponseEntity.ok(DataResponseBody(configurationService.findAll(PageSearchRequest(page, size))))

    @Operation(summary = "설정 상세 조회", description = "ID로 특정 설정의 상세 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "설정 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "설정을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun getConfiguration(
        @PathVariable
        @Parameter(description = "설정 ID", required = true)
        id: Long,
    ): ResponseEntity<DataResponseBody<ConfigurationResponse>> = ResponseEntity.ok(DataResponseBody(configurationService.findById(id)))

    @Operation(summary = "설정 수정", description = "설정 값을 수정합니다. 키는 불변입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "설정 수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "설정을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping("/{id}")
    fun updateConfiguration(
        @PathVariable
        @Parameter(description = "설정 ID", required = true)
        id: Long,
        @Parameter(description = "설정 수정 정보", required = true)
        @RequestBody
        @Valid
        request: ConfigurationUpdateRequest,
    ): ResponseEntity<Void> {
        configurationService.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "설정 삭제", description = "설정을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "설정 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "설정을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteConfiguration(
        @PathVariable
        @Parameter(description = "설정 ID", required = true)
        id: Long,
    ): ResponseEntity<Void> {
        configurationService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
