package com.pluxity.yongin.goal.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.goal.dto.dummyGoalBulkRequest
import com.pluxity.yongin.goal.dto.dummyGoalResponse
import com.pluxity.yongin.goal.service.GoalService
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

@WebMvcTest(GoalController::class)
class GoalControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: GoalService,
) : BehaviorSpec({

        val baseUrl = "/goals"

        Given("목표관리 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummyGoalResponse()),
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
                        param("size", "10")
                        with(user("tester"))
                    }

                Then("200 OK와 페이징된 목표관리 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.content.length()") { value(1) }
                        jsonPath("$.data.content[0].id") { value(1) }
                        jsonPath("$.data.pageNumber") { value(1) }
                        jsonPath("$.data.pageSize") { value(10) }
                        jsonPath("$.data.totalElements") { value(1) }
                    }
                }
            }
        }

        Given("최신 목표관리 조회 API") {

            When("GET $baseUrl/latest - 성공") {
                every { service.findLatest() } returns listOf(dummyGoalResponse())

                val result =
                    mockMvc.get("$baseUrl/latest") {
                        with(user("tester"))
                    }

                Then("200 OK와 최신 목표관리 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                    }
                }
            }
        }

        Given("목표관리 일괄 저장 API") {

            When("PUT $baseUrl - 유효한 요청") {
                val request = dummyGoalBulkRequest()

                every { service.saveOrUpdateAll(any()) } just runs

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

        Given("존재하지 않는 목표관리로 일괄 저장 요청하면") {
            every { service.saveOrUpdateAll(any()) } throws CustomException(YonginErrorCode.NOT_FOUND_GOAL, 999L)

            When("PUT $baseUrl 요청 시") {
                val request = dummyGoalBulkRequest()

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
