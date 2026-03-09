package com.pluxity.yongin.keymanagement.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.aop.ResponseCreatedAspect
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.keymanagement.dto.dummyKeyManagementGroupResponse
import com.pluxity.yongin.keymanagement.dto.dummyKeyManagementRequest
import com.pluxity.yongin.keymanagement.dto.dummyKeyManagementResponse
import com.pluxity.yongin.keymanagement.dto.dummyKeyManagementUpdateRequest
import com.pluxity.yongin.keymanagement.entity.KeyManagementType
import com.pluxity.yongin.keymanagement.service.KeyManagementService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper

@WebMvcTest(KeyManagementController::class)
@Import(ResponseCreatedAspect::class)
@EnableAspectJAutoProxy
class KeyManagementControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: KeyManagementService,
) : BehaviorSpec({

        val baseUrl = "/key-management"

        Given("주요관리사항 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyKeyManagementGroupResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 그룹화된 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].type") { value("QUALITY") }
                        jsonPath("$.data[0].typeDescription") { value("품질") }
                        jsonPath("$.data[0].items") { isArray() }
                    }
                }
            }
        }

        Given("대시보드 선택 항목 조회 API") {

            When("GET $baseUrl/selected - 성공") {
                every { service.findSelected() } returns listOf(dummyKeyManagementResponse())

                val result =
                    mockMvc.get("$baseUrl/selected") {
                        with(user("tester"))
                    }

                Then("200 OK와 선택된 항목 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                    }
                }
            }
        }

        Given("주요관리사항 단건 조회 API") {

            When("GET $baseUrl/1 - 존재하는 항목") {
                every { service.findById(1L) } returns dummyKeyManagementResponse()

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 주요관리사항이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.title") { value("테스트 제목") }
                    }
                }
            }

            When("GET $baseUrl/999 - 존재하지 않는 항목") {
                every { service.findById(999L) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_KEY_MANAGEMENT, 999L)

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

        Given("주요관리사항 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyKeyManagementRequest()

                every { service.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("201 Created와 ID가 반환된다") {
                    result.andExpect {
                        status { isCreated() }
                        header { string("Location", "/key-management/1") }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest = dummyKeyManagementRequest(title = "")

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

        Given("주요관리사항 수정 API") {

            When("PUT $baseUrl/1 - 유효한 요청") {
                val request = dummyKeyManagementUpdateRequest()

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

            When("PUT $baseUrl/999 - 존재하지 않는 항목") {
                val request = dummyKeyManagementUpdateRequest()

                every { service.update(999L, any()) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_KEY_MANAGEMENT, 999L)

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

        Given("주요관리사항 삭제 API") {

            When("DELETE $baseUrl/1 - 성공") {
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

        Given("주요관리사항 타입 목록 조회 API") {

            When("GET $baseUrl/types - 타입 목록 조회") {
                val result =
                    mockMvc.get("$baseUrl/types") {
                        with(user("tester"))
                    }

                Then("200 OK와 타입 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(KeyManagementType.entries.size) }
                    }
                }
            }
        }

        Given("대시보드 선택/해제 API") {

            When("PATCH $baseUrl/1/select - 성공") {
                every { service.select(1L) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1/select") {
                        with(csrf())
                        with(user("tester"))
                    }

                Then("204 No Content가 반환된다") {
                    result.andExpect {
                        status { isNoContent() }
                    }
                }
            }

            When("PATCH $baseUrl/1/deselect - 성공") {
                every { service.deselect(1L) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1/deselect") {
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
