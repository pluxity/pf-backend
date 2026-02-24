package com.pluxity.common.file.controller

import com.pluxity.common.core.annotation.ResponseCreated
import com.pluxity.common.core.response.DataResponseBody
import com.pluxity.common.core.response.ErrorResponseBody
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.file.service.FileService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/files")
@Tag(name = "File Controller", description = "파일 관리 API")
class FileController(
    private val fileService: FileService,
) {
    @Operation(summary = "사전 서명된 URL 생성", description = "S3 Key로 사전 서명된 URL을 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "URL 생성 성공",
            ),
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
    @GetMapping("/pre-signed-url")
    fun getPreSignedUrl(
        @Parameter(description = "S3 버킷 키", required = true) @RequestParam s3Key: String,
    ): ResponseEntity<DataResponseBody<String>> {
        val url = fileService.generatePreSignedUrl(s3Key)
        return ResponseEntity.ok(DataResponseBody(url))
    }

    @Operation(summary = "파일 업로드", description = "새로운 파일을 업로드합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "파일 업로드 성공",
                content = [Content(mediaType = "application/json")],
            ),
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
    @PostMapping(value = ["/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseCreated(path = "/files/{id}")
    fun uploadFile(
        @Parameter(description = "업로드할 파일", required = true) @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Long> = ResponseEntity.ok(fileService.initiateUpload(file))

    @Operation(summary = "파일 정보 조회", description = "ID로 파일 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "파일 정보 조회 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "파일을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
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
    @GetMapping("/{id}")
    fun getFileInfo(
        @Parameter(description = "파일 ID", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<FileResponse>> {
        val totalStart = System.currentTimeMillis()
        log.info { "[getFileInfo] started for id=$id" }

        val serviceStart = System.currentTimeMillis()
        val fileResponse = fileService.getFileResponse(id)
        log.info { "[getFileInfo] service call took ${System.currentTimeMillis() - serviceStart}ms" }

        val responseStart = System.currentTimeMillis()
        val response = ResponseEntity.ok(DataResponseBody(fileResponse))
        log.info { "[getFileInfo] response build took ${System.currentTimeMillis() - responseStart}ms" }

        log.info { "[getFileInfo] total took ${System.currentTimeMillis() - totalStart}ms" }
        return response
    }
}
