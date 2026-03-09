package com.pluxity.common.file.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.file.constant.FileStatus
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.file.service.FileService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.web.multipart.MultipartFile

@WebMvcTest(FileController::class)
class FileControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val fileService: FileService,
) : BehaviorSpec({

        val fileResponse =
            FileResponse(
                id = 1L,
                url = "http://localhost/files/test.png",
                originalFileName = "test.png",
                contentType = "image/png",
                fileStatus = FileStatus.COMPLETE.toString(),
                zipContents = emptyList(),
                baseResponse =
                    BaseResponse(
                        createdAt = "2026-01-01T00:00:00",
                        createdBy = "system",
                        updatedAt = "2026-01-01T00:00:00",
                        updatedBy = "system",
                    ),
            )

        Given("사전 서명된 URL 생성 API") {

            When("GET /files/pre-signed-url - 유효한 요청") {
                every { fileService.generatePreSignedUrl("some-key") } returns "https://s3.example.com/pre-signed"

                val result =
                    mockMvc.get("/files/pre-signed-url") {
                        param("s3Key", "some-key")
                        with(user("tester"))
                    }

                Then("200 OK와 사전 서명된 URL이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { value("https://s3.example.com/pre-signed") }
                    }
                }
            }
        }

        Given("파일 업로드 API") {

            When("POST /files/upload - 유효한 파일") {
                val file =
                    MockMultipartFile(
                        "file",
                        "test.png",
                        MediaType.IMAGE_PNG_VALUE,
                        "test-content".toByteArray(),
                    )

                every { fileService.initiateUpload(any<MultipartFile>()) } returns 1L

                val result =
                    mockMvc.multipart("/files/upload") {
                        file(file)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 파일 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        content { string("1") }
                    }
                }
            }
        }

        Given("파일 정보 조회 API") {

            When("GET /files/{id} - 존재하는 파일") {
                every { fileService.getFileResponse(1L) } returns fileResponse

                val result =
                    mockMvc.get("/files/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 파일 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.url") { value("http://localhost/files/test.png") }
                        jsonPath("$.data.originalFileName") { value("test.png") }
                        jsonPath("$.data.contentType") { value("image/png") }
                        jsonPath("$.data.fileStatus") { value("COMPLETE") }
                    }
                }
            }

            When("GET /files/{id} - 존재하지 않는 파일") {
                every { fileService.getFileResponse(999L) } throws
                    CustomException(ErrorCode.NOT_FOUND_FILE, 999L)

                val result =
                    mockMvc.get("/files/999") {
                        with(user("tester"))
                    }

                Then("404 Not Found가 반환된다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }
        }
    })
