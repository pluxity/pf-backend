package com.pluxity.yongin.notice.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.notice.dto.dummyNoticeRequest
import com.pluxity.yongin.notice.dto.dummyNoticeResponse
import com.pluxity.yongin.notice.service.NoticeService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper

@WebMvcTest(NoticeController::class)
class NoticeControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: NoticeService,
) : BehaviorSpec({

        val baseUrl = "/notices"

        Given("공지사항 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummyNoticeResponse()),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 1,
                        last = true,
                        first = true,
                    )

                every { service.findAll(any()) } returns pageResponse

                val result =
                    mockMvc.get(baseUrl) {
                        param("page", "1")
                        param("size", "9999")
                        with(user("tester"))
                    }

                Then("200 OK와 공지사항 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.content.length()") { value(1) }
                        jsonPath("$.data.content[0].id") { value(1) }
                        jsonPath("$.data.content[0].title") { value("테스트 공지사항") }
                    }
                }
            }
        }

        Given("활성 공지사항 조회 API") {

            When("GET $baseUrl/active - 성공") {
                every { service.findActive() } returns listOf(dummyNoticeResponse())

                val result =
                    mockMvc.get("$baseUrl/active") {
                        with(user("tester"))
                    }

                Then("200 OK와 활성 공지사항 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                    }
                }
            }
        }

        Given("공지사항 단건 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 공지사항") {
                every { service.findById(1L) } returns dummyNoticeResponse()

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 공지사항이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.title") { value("테스트 공지사항") }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 공지사항") {
                every { service.findById(999L) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_NOTICE, 999L)

                val result =
                    mockMvc.get("$baseUrl/999") {
                        with(user("tester"))
                    }

                Then("404 Not Found가 반환된다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }
        }

        Given("공지사항 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyNoticeRequest()

                every { service.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("201 Created가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$") { value(1) }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest = dummyNoticeRequest(title = "")

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(invalidRequest)
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

        Given("공지사항 수정 API") {

            When("PUT $baseUrl/{id} - 유효한 요청") {
                val request = dummyNoticeRequest()

                every { service.update(1L, any()) } just runs

                val result =
                    mockMvc.put("$baseUrl/1") {
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

            When("PUT $baseUrl/{id} - 존재하지 않는 공지사항") {
                val request = dummyNoticeRequest()

                every { service.update(999L, any()) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_NOTICE, 999L)

                val result =
                    mockMvc.put("$baseUrl/999") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("404 Not Found가 반환된다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }
        }

        Given("공지사항 삭제 API") {

            When("DELETE $baseUrl/{id} - 성공") {
                every { service.delete(1L) } just runs

                val result =
                    mockMvc.delete("$baseUrl/1") {
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
    })
