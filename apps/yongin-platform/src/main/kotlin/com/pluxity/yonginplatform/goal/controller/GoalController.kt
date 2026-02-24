package com.pluxity.yonginplatform.goal.controller

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yonginplatform.goal.dto.GoalBulkRequest
import com.pluxity.yonginplatform.goal.dto.GoalResponse
import com.pluxity.yonginplatform.goal.service.GoalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/goals")
@Tag(name = "Goal Controller", description = "목표관리 API")
class GoalController(
    private val service: GoalService,
) {
    @Operation(summary = "목표관리 전체 조회", description = "목표관리 전체 목록을 조회합니다")
    @GetMapping
    fun findAll(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page") page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "9999")
        @RequestParam("size") size: Int = 9999,
    ): ResponseEntity<DataResponseBody<PageResponse<GoalResponse>>> =
        ResponseEntity.ok(DataResponseBody(service.findAll(PageSearchRequest(page, size))))

    @Operation(summary = "최근 목표관리 조회", description = "가장 최근 입력일자의 목표관리 목록을 조회합니다")
    @GetMapping("/latest")
    fun findLatest(): ResponseEntity<DataResponseBody<List<GoalResponse>>> = ResponseEntity.ok(DataResponseBody(service.findLatest()))

    @Operation(summary = "목표관리 저장/수정/삭제", description = "목표관리를 저장, 수정, 삭제합니다. upserts의 id가 없으면 생성, 있으면 수정합니다. deletedIds에 포함된 id는 삭제됩니다")
    @PutMapping
    fun saveOrUpdateAll(
        @RequestBody @Valid request: GoalBulkRequest,
    ): ResponseEntity<Void> {
        service.saveOrUpdateAll(request)
        return ResponseEntity.noContent().build()
    }
}
