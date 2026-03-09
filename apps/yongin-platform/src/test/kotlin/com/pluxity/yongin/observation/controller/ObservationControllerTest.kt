package com.pluxity.yongin.observation.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.aop.ResponseCreatedAspect
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.observation.dto.dummyObservationRequest
import com.pluxity.yongin.observation.dto.dummyObservationResponse
import com.pluxity.yongin.observation.service.ObservationService
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper

@WebMvcTest(ObservationController::class)
@Import(ResponseCreatedAspect::class)
@EnableAspectJAutoProxy
class ObservationControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: ObservationService,
) : BehaviorSpec({

        val baseUrl = "/observations"

        Given("드론 관측 데이터 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyObservationResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 드론 관측 데이터 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].rootFileName") { value("test-model") }
                    }
                }
            }
        }

        Given("드론 관측 데이터 단건 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 데이터") {
                every { service.findById(1L) } returns dummyObservationResponse()

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 드론 관측 데이터가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.rootFileName") { value("test-model") }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 데이터") {
                every { service.findById(999L) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_OBSERVATION, 999L)

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

        Given("드론 관측 데이터 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyObservationRequest()

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
                        status { isCreated() }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest = dummyObservationRequest(rootFileName = "")

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

        Given("드론 관측 데이터 수정 API") {

            When("PUT $baseUrl/{id} - 유효한 요청") {
                val request = dummyObservationRequest()

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

            When("PUT $baseUrl/{id} - 존재하지 않는 데이터") {
                val request = dummyObservationRequest()

                every { service.update(999L, any()) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_OBSERVATION, 999L)

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

        Given("드론 관측 데이터 삭제 API") {

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
