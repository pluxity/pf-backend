package com.pluxity.yongin.processstatus.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusBulkRequest
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusImageRequest
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusImageResponse
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusResponse
import com.pluxity.yongin.processstatus.service.ProcessStatusImageService
import com.pluxity.yongin.processstatus.service.ProcessStatusService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper

@WebMvcTest(ProcessStatusController::class)
class ProcessStatusControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val processStatusService: ProcessStatusService,
    @MockkBean private val processStatusImageService: ProcessStatusImageService,
) : BehaviorSpec({

        val baseUrl = "/process-statuses"

        Given("공정현황 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummyProcessStatusResponse()),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 1,
                        last = true,
                        first = true,
                    )

                every { processStatusService.findAll(any()) } returns pageResponse

                val result =
                    mockMvc.get(baseUrl) {
                        param("page", "1")
                        param("size", "9999")
                        with(user("tester"))
                    }

                Then("200 OK와 공정현황 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.content.length()") { value(1) }
                        jsonPath("$.data.content[0].id") { value(1) }
                    }
                }
            }
        }

        Given("최신 공정현황 조회 API") {

            When("GET $baseUrl/latest - 성공") {
                every { processStatusService.findLatest() } returns listOf(dummyProcessStatusResponse())

                val result =
                    mockMvc.get("$baseUrl/latest") {
                        with(user("tester"))
                    }

                Then("200 OK와 최신 공정현황 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                    }
                }
            }
        }

        Given("공정현황 일괄 저장 API") {

            When("PUT $baseUrl - 유효한 요청") {
                val request = dummyProcessStatusBulkRequest()

                every { processStatusService.saveOrUpdateAll(any()) } just runs

                val result =
                    mockMvc.put(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("204 No Content가 반환된다") {
                    result.andExpect {
                        status { isNoContent() }
                    }
                }
            }
        }

        Given("공정 이미지 조회 API") {

            When("GET $baseUrl/image - 성공") {
                every { processStatusImageService.getImage() } returns dummyProcessStatusImageResponse()

                val result =
                    mockMvc.get("$baseUrl/image") {
                        with(user("tester"))
                    }

                Then("200 OK와 공정 이미지 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.fileId") { value(1) }
                    }
                }
            }
        }

        Given("공정 이미지 수정 API") {

            When("PUT $baseUrl/image - 유효한 요청") {
                val request = dummyProcessStatusImageRequest()

                every { processStatusImageService.saveImage(any()) } just runs

                val result =
                    mockMvc.put("$baseUrl/image") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("204 No Content가 반환된다") {
                    result.andExpect {
                        status { isNoContent() }
                    }
                }
            }
        }

        Given("존재하지 않는 공정명으로 일괄 저장 요청하면") {
            every { processStatusService.saveOrUpdateAll(any()) } throws CustomException(YonginErrorCode.NOT_FOUND_WORK_TYPE, 999L)

            When("PUT $baseUrl 요청 시") {
                val request = dummyProcessStatusBulkRequest()

                val result =
                    mockMvc.put(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("404 Not Found를 반환한다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }
        }
    })
