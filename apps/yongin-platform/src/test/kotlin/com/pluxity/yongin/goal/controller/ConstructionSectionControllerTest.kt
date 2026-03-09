package com.pluxity.yongin.goal.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.aop.ResponseCreatedAspect
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.goal.dto.dummyConstructionSectionRequest
import com.pluxity.yongin.goal.dto.dummyConstructionSectionResponse
import com.pluxity.yongin.goal.service.ConstructionSectionService
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
import tools.jackson.databind.ObjectMapper

@WebMvcTest(ConstructionSectionController::class)
@Import(ResponseCreatedAspect::class)
@EnableAspectJAutoProxy
class ConstructionSectionControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: ConstructionSectionService,
) : BehaviorSpec({

        val baseUrl = "/goals/construction-sections"

        Given("시공구간 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyConstructionSectionResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 시공구간 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("절토") }
                    }
                }
            }
        }

        Given("시공구간 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyConstructionSectionRequest()

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
                        header { string("Location", "/goals/construction-sections/1") }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val request = dummyConstructionSectionRequest(name = "")

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

        Given("시공구간 삭제 API") {

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

        Given("존재하지 않는 ID로 시공구간 삭제 요청하면") {
            every { service.delete(any()) } throws CustomException(YonginErrorCode.NOT_FOUND_CONSTRUCTION_SECTION, 999L)

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

        Given("목표관리가 등록된 시공구간을 삭제 요청하면") {
            every { service.delete(any()) } throws CustomException(YonginErrorCode.CONSTRUCTION_SECTION_HAS_GOAL)

            When("DELETE $baseUrl/{id} 요청 시") {
                val result =
                    mockMvc.delete("$baseUrl/1") {
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
    })
