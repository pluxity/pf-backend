package com.pluxity.yongin.processstatus.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.aop.ResponseCreatedAspect
import com.pluxity.yongin.processstatus.dto.dummyWorkTypeRequest
import com.pluxity.yongin.processstatus.dto.dummyWorkTypeResponse
import com.pluxity.yongin.processstatus.service.WorkTypeService
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

@WebMvcTest(WorkTypeController::class)
@Import(ResponseCreatedAspect::class)
@EnableAspectJAutoProxy
class WorkTypeControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: WorkTypeService,
) : BehaviorSpec({

        val baseUrl = "/process-statuses/work-types"

        Given("공정명 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyWorkTypeResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 공정명 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("토공") }
                    }
                }
            }
        }

        Given("공정명 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyWorkTypeRequest()

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
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val request = dummyWorkTypeRequest(name = "")

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

        Given("공정명 삭제 API") {

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
