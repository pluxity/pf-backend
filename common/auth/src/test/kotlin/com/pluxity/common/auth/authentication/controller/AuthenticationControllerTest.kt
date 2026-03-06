package com.pluxity.common.auth.authentication.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.authentication.service.AuthenticationService
import com.pluxity.common.auth.test.dummySignInRequest
import com.pluxity.common.auth.test.dummySignUpRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AuthenticationController::class)
class AuthenticationControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val authenticationService: AuthenticationService,
) : BehaviorSpec({

        val baseUrl = "/auth"

        Given("회원가입 API") {

            When("POST $baseUrl/sign-up - 유효한 요청") {
                val request = dummySignUpRequest()

                every { authenticationService.signUp(any()) } returns 1L

                val result =
                    mockMvc.post("$baseUrl/sign-up") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 생성된 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        content { string("1") }
                    }
                }
            }

            When("POST $baseUrl/sign-up - 유효하지 않은 요청 (빈 필드)") {
                val invalidRequest =
                    mapOf(
                        "username" to "",
                        "password" to "",
                        "name" to "",
                        "code" to "",
                    )

                val result =
                    mockMvc.post("$baseUrl/sign-up") {
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

        Given("로그인 API") {

            When("POST $baseUrl/sign-in - 유효한 요청") {
                val request = dummySignInRequest()

                every { authenticationService.signIn(any(), any(), any()) } just runs

                val result =
                    mockMvc.post("$baseUrl/sign-in") {
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

            When("POST $baseUrl/sign-in - 유효하지 않은 요청 (빈 필드)") {
                val invalidRequest =
                    mapOf(
                        "username" to "",
                        "password" to "",
                    )

                val result =
                    mockMvc.post("$baseUrl/sign-in") {
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

        Given("로그아웃 API") {

            When("POST $baseUrl/sign-out - 유효한 요청") {
                every { authenticationService.signOut(any(), any()) } just runs

                val result =
                    mockMvc.post("$baseUrl/sign-out") {
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

        Given("토큰 갱신 API") {

            When("POST $baseUrl/refresh-token - 유효한 요청") {
                every { authenticationService.refreshToken(any(), any()) } just runs

                val result =
                    mockMvc.post("$baseUrl/refresh-token") {
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
