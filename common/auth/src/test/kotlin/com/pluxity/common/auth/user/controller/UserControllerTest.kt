package com.pluxity.common.auth.user.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.user.dto.UserResponse
import com.pluxity.common.auth.user.service.UserService
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.test.dto.dummyUserPasswordUpdateRequest
import com.pluxity.common.test.dto.dummyUserUpdateRequest
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
import tools.jackson.databind.ObjectMapper

@WebMvcTest(UserController::class)
class UserControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: UserService,
) : BehaviorSpec({

        val baseUrl = "/users"

        val userResponse =
            UserResponse(
                id = 1L,
                username = "tester",
                name = "테스트",
                code = "CODE01",
                phoneNumber = null,
                department = null,
                profileImage = FileResponse(),
                shouldChangePassword = false,
                roles = emptyList(),
            )

        Given("내 정보 조회 API") {

            When("GET $baseUrl/me - 인증된 사용자") {
                every { service.findByUsername("tester") } returns userResponse

                val result =
                    mockMvc.get("$baseUrl/me") {
                        with(user("tester"))
                    }

                Then("200 OK와 사용자 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.username") { value("tester") }
                        jsonPath("$.data.name") { value("테스트") }
                        jsonPath("$.data.code") { value("CODE01") }
                    }
                }
            }
        }

        Given("내 정보 수정 API") {

            When("PATCH $baseUrl/me - 유효한 요청") {
                val request = dummyUserUpdateRequest()

                every { service.findByUsername("tester") } returns userResponse
                every { service.update(1L, any()) } returns userResponse

                val result =
                    mockMvc.patch("$baseUrl/me") {
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

        Given("내 비밀번호 변경 API") {

            When("PATCH $baseUrl/me/password - 유효한 요청") {
                val request = dummyUserPasswordUpdateRequest()

                every { service.updateUserPassword(eq("tester"), any()) } just runs

                val result =
                    mockMvc.patch("$baseUrl/me/password") {
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
