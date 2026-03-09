package com.pluxity.yongin.cctv.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.dto.dummyCctvBookmarkOrderRequest
import com.pluxity.yongin.cctv.dto.dummyCctvBookmarkRequest
import com.pluxity.yongin.cctv.dto.dummyCctvBookmarkResponse
import com.pluxity.yongin.cctv.service.CctvBookmarkService
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(CctvBookmarkController::class)
class CctvBookmarkControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: CctvBookmarkService,
) : BehaviorSpec({

        val baseUrl = "/cctv-bookmarks"

        Given("CCTV 즐겨찾기 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyCctvBookmarkResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 즐겨찾기 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].streamName") { value("CCTV-001") }
                    }
                }
            }
        }

        Given("CCTV 즐겨찾기 등록 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyCctvBookmarkRequest()

                every { service.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 생성된 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { value(1) }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val request = dummyCctvBookmarkRequest(streamName = "")

                val result =
                    mockMvc.post(baseUrl) {
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

        Given("CCTV 즐겨찾기 삭제 API") {

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

        Given("CCTV 즐겨찾기 순서 변경 API") {

            When("PATCH $baseUrl/order - 유효한 요청") {
                val request = dummyCctvBookmarkOrderRequest()

                every { service.updateOrder(any()) } just runs

                val result =
                    mockMvc.patch("$baseUrl/order") {
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

        Given("존재하지 않는 ID로 즐겨찾기 삭제 요청하면") {
            every { service.delete(any()) } throws CustomException(YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK, 999L)

            When("DELETE $baseUrl/{id} 요청 시") {
                val result =
                    mockMvc.delete("$baseUrl/999") {
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

        Given("이미 즐겨찾기된 CCTV를 등록 요청하면") {
            every { service.create(any()) } throws CustomException(YonginErrorCode.ALREADY_BOOKMARK)

            When("POST $baseUrl 요청 시") {
                val request = dummyCctvBookmarkRequest()

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("400 Bad Request를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }

        Given("즐겨찾기 개수 제한을 초과하여 등록 요청하면") {
            every { service.create(any()) } throws CustomException(YonginErrorCode.EXCEED_BOOKMARK_LIMIT)

            When("POST $baseUrl 요청 시") {
                val request = dummyCctvBookmarkRequest()

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("400 Bad Request를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }

        Given("존재하지 않는 즐겨찾기로 순서 변경 요청하면") {
            every { service.updateOrder(any()) } throws CustomException(YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK, 999L)

            When("PATCH $baseUrl/order 요청 시") {
                val request = dummyCctvBookmarkOrderRequest()

                val result =
                    mockMvc.patch("$baseUrl/order") {
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
