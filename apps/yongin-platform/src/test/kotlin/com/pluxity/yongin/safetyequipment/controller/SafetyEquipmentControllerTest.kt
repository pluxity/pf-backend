package com.pluxity.yongin.safetyequipment.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.safetyequipment.dto.dummySafetyEquipmentRequest
import com.pluxity.yongin.safetyequipment.dto.dummySafetyEquipmentResponse
import com.pluxity.yongin.safetyequipment.service.SafetyEquipmentService
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

@WebMvcTest(SafetyEquipmentController::class)
class SafetyEquipmentControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val safetyEquipmentService: SafetyEquipmentService,
) : BehaviorSpec({

        val baseUrl = "/safety-equipments"

        Given("안전장비 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { safetyEquipmentService.findAll() } returns listOf(dummySafetyEquipmentResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 안전장비 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("안전모") }
                    }
                }
            }
        }

        Given("안전장비 단건 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 장비") {
                every { safetyEquipmentService.findById(1L) } returns dummySafetyEquipmentResponse()

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 안전장비 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.name") { value("안전모") }
                        jsonPath("$.data.quantity") { value(100) }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 장비") {
                every { safetyEquipmentService.findById(999L) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT, 999L)

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

        Given("안전장비 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummySafetyEquipmentRequest()

                every { safetyEquipmentService.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$") { value(1) }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest = dummySafetyEquipmentRequest(name = "")

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

        Given("안전장비 수정 API") {

            When("PUT $baseUrl/{id} - 유효한 요청") {
                val request = dummySafetyEquipmentRequest()

                every { safetyEquipmentService.update(1L, any()) } just runs

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

            When("PUT $baseUrl/{id} - 존재하지 않는 장비") {
                val request = dummySafetyEquipmentRequest()

                every { safetyEquipmentService.update(999L, any()) } throws
                    CustomException(YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT, 999L)

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

        Given("안전장비 삭제 API") {

            When("DELETE $baseUrl/{id} - 성공") {
                every { safetyEquipmentService.delete(1L) } just runs

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
