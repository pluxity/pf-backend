package com.pluxity.yongin.attendance.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.attendance.dto.dummyAttendanceResponse
import com.pluxity.yongin.attendance.dto.dummyAttendanceUpdateRequest
import com.pluxity.yongin.attendance.service.AttendanceFacade
import com.pluxity.yongin.global.constant.YonginErrorCode
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
import org.springframework.test.web.servlet.patch
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AttendanceController::class)
class AttendanceControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: AttendanceFacade,
) : BehaviorSpec({

        val baseUrl = "/attendances"

        Given("출역현황 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummyAttendanceResponse()),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 1,
                        last = true,
                        first = true,
                    )

                every { service.findAllWithSync(any()) } returns pageResponse

                val result =
                    mockMvc.get(baseUrl) {
                        param("page", "1")
                        param("size", "9999")
                        with(user("tester"))
                    }

                Then("200 OK와 출역현황 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.content.length()") { value(1) }
                        jsonPath("$.data.content[0].id") { value(1) }
                        jsonPath("$.data.content[0].deviceName") { value("입구 게이트") }
                    }
                }
            }
        }

        Given("최신 출역현황 조회 API") {

            When("GET $baseUrl/latest - 성공") {
                every { service.findLatest() } returns listOf(dummyAttendanceResponse())

                val result =
                    mockMvc.get("$baseUrl/latest") {
                        with(user("tester"))
                    }

                Then("200 OK와 최신 출역현황 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                    }
                }
            }
        }

        Given("출역현황 작업내용 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyAttendanceUpdateRequest()

                every { service.updateWorkContent(1L, any()) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1") {
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

            When("PATCH $baseUrl/{id} - 필수 필드 누락") {
                val request = dummyAttendanceUpdateRequest(workContent = "")

                val result =
                    mockMvc.patch("$baseUrl/1") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("400 Bad Request가 반환된다") {
                    result.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }

        Given("존재하지 않는 ID로 작업내용 수정 요청하면") {
            every { service.updateWorkContent(any(), any()) } throws CustomException(YonginErrorCode.NOT_FOUND_ATTENDANCE, 999L)

            When("PATCH $baseUrl/{id} 요청 시") {
                val request = dummyAttendanceUpdateRequest()

                val result =
                    mockMvc.patch("$baseUrl/999") {
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
