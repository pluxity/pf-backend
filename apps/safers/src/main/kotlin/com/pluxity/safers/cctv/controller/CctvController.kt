package com.pluxity.safers.cctv.controller

import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.safers.cctv.dto.CctvPlaybackRequest
import com.pluxity.safers.cctv.dto.CctvPlaybackResponse
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.service.CctvFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctvs")
@Tag(name = "CCTV Controller", description = "CCTV 관리 API")
class CctvController(
    private val service: CctvFacade,
) {
    @Operation(summary = "CCTV 동기화", description = "미디어서버에서 CCTV 경로 목록을 가져와 DB에 동기화합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "동기화 성공"),
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
    @PostMapping("/sync")
    fun sync(
        @RequestParam(required = false) siteId: Long?,
    ): ResponseEntity<Void> {
        service.sync(siteId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "CCTV 목록 조회", description = "CCTV 목록을 조회합니다 (이름순 정렬)")
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
        @RequestParam(required = false) siteId: Long?,
    ): ResponseEntity<DataResponseBody<List<CctvResponse>>> = ResponseEntity.ok(DataResponseBody(service.findAll(siteId)))

    @Operation(summary = "CCTV 수정", description = "CCTV의 이름, 경도, 위도를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "CCTV를 찾을 수 없음",
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
    fun update(
        @PathVariable id: Long,
        @RequestBody @Valid request: CctvUpdateRequest,
    ): ResponseEntity<Void> {
        service.update(id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "NVR 녹화영상 재생 세션 생성", description = "NVR 녹화영상의 재생 스트림 경로를 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "재생 세션 생성 성공"),
            ApiResponse(
                responseCode = "400",
                description = "NVR 정보 누락 또는 미디어서버 URL 미설정",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CCTV를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{id}/playback")
    fun playback(
        @PathVariable id: Long,
        @RequestBody @Valid request: CctvPlaybackRequest,
    ): ResponseEntity<DataResponseBody<CctvPlaybackResponse>> = ResponseEntity.ok(DataResponseBody(service.playback(id, request)))

    @Operation(summary = "NVR 녹화영상 재생 세션 삭제", description = "생성된 NVR 녹화영상 재생 세션을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "400",
                description = "NVR 정보 누락 또는 미디어서버 URL 미설정",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CCTV를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/{id}/playback/{pathName}")
    fun deletePlayback(
        @PathVariable id: Long,
        @PathVariable pathName: String,
    ): ResponseEntity<Void> {
        service.deletePlayback(id, pathName)
        return ResponseEntity.noContent().build()
    }
}
