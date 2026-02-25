package com.pluxity.yongin.announcement.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.yongin.announcement.dto.AnnouncementRequest
import com.pluxity.yongin.announcement.dto.AnnouncementResponse
import com.pluxity.yongin.announcement.service.AnnouncementService
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/announcement")
@Tag(name = "Announcement Controller", description = "안내사항 관리 API")
class AnnouncementController(
    private val service: AnnouncementService,
) {
    @Operation(summary = "안내사항 조회", description = "안내사항을 조회합니다")
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
    fun find(): ResponseEntity<DataResponseBody<AnnouncementResponse>> = ResponseEntity.ok(DataResponseBody(service.getAnnouncement()))

    @Operation(summary = "안내사항 수정", description = "안내사항을 수정합니다 (없으면 생성)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "수정 성공"),
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
        ],
    )
    @PutMapping
    fun upsert(
        @RequestBody @Valid request: AnnouncementRequest,
    ): ResponseEntity<Void> {
        service.saveAnnouncement(request)
        return ResponseEntity.noContent().build()
    }
}
