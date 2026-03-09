package com.pluxity.yongin.cctv.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.yongin.cctv.dto.dummyCctvResponse
import com.pluxity.yongin.cctv.dto.dummyCctvUpdateRequest
import com.pluxity.yongin.cctv.service.CctvService
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
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(CctvController::class)
class CctvControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: CctvService,
) : BehaviorSpec({

        val baseUrl = "/cctvs"

        Given("CCTV 동기화 API") {

            When("POST $baseUrl/sync - 성공") {
                every { service.sync() } just runs

                val result =
                    mockMvc.post("$baseUrl/sync") {
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

        Given("CCTV 목록 조회 API") {

            When("GET $baseUrl - 성공") {
                every { service.findAll() } returns listOf(dummyCctvResponse())

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 CCTV 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("1번 카메라") }
                    }
                }
            }
        }

        Given("CCTV 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyCctvUpdateRequest()

                every { service.update(1L, any()) } just runs

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
        }
    })
