package com.pluxity.yongin.systemsetting.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.yongin.systemsetting.dto.dummySystemSettingRequest
import com.pluxity.yongin.systemsetting.dto.dummySystemSettingResponse
import com.pluxity.yongin.systemsetting.service.SystemSettingService
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

@WebMvcTest(SystemSettingController::class)
class SystemSettingControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val systemSettingService: SystemSettingService,
) : BehaviorSpec({

        Given("시스템 설정 조회 API") {

            When("GET /system-settings - 성공") {
                val response = dummySystemSettingResponse()

                every { systemSettingService.find() } returns response

                val result =
                    mockMvc.get("/system-settings") {
                        with(user("tester"))
                    }

                Then("200 OK와 시스템 설정이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.rollingIntervalSeconds") { value(30) }
                    }
                }
            }
        }

        Given("시스템 설정 수정 API") {

            When("PUT /system-settings - 유효한 요청") {
                val request = dummySystemSettingRequest()

                every { systemSettingService.update(any()) } just runs

                val result =
                    mockMvc.put("/system-settings") {
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
