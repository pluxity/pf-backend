package com.pluxity.common.auth.user.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.user.dto.UserLoggedInResponse
import com.pluxity.common.auth.user.dto.UserResponse
import com.pluxity.common.auth.user.service.UserService
import com.pluxity.common.test.dto.dummyUserCreateRequest
import com.pluxity.common.test.dto.dummyUserPasswordUpdateRequest
import com.pluxity.common.test.dto.dummyUserRoleAssignRequest
import com.pluxity.common.test.dto.dummyUserUpdateRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import jakarta.persistence.EntityNotFoundException
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AdminUserController::class)
class AdminUserControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: UserService,
) : BehaviorSpec({

        val baseUrl = "/admin/users"

        val userResponse =
            UserResponse(
                id = 1L,
                username = "testuser",
                name = "테스트",
                code = "CODE01",
                phoneNumber = null,
                department = null,
                shouldChangePassword = false,
                roles = emptyList(),
            )

        Given("사용자 목록 조회 API") {

            When("GET $baseUrl - 사용자 목록 조회") {
                every { service.findAll() } returns listOf(userResponse)

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 사용자 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].username") { value("testuser") }
                    }
                }
            }

            When("GET $baseUrl - 빈 목록") {
                every { service.findAll() } returns emptyList()

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 빈 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(0) }
                    }
                }
            }
        }

        Given("사용자 상세 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 사용자") {
                every { service.findById(1L) } returns userResponse

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 사용자 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.username") { value("testuser") }
                        jsonPath("$.data.name") { value("테스트") }
                        jsonPath("$.data.code") { value("CODE01") }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 사용자") {
                every { service.findById(999L) } throws
                    EntityNotFoundException("사용자를 찾을 수 없습니다.")

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

        Given("로그인된 사용자 정보 조회 API") {

            When("GET $baseUrl/with-is-logged-in - 조회") {
                val response =
                    UserLoggedInResponse(
                        id = 1L,
                        username = "testuser",
                        name = "테스트",
                        code = "CODE01",
                        phoneNumber = null,
                        department = null,
                        isLoggedIn = true,
                        roles = emptyList(),
                    )

                every { service.isLoggedIn() } returns listOf(response)

                val result =
                    mockMvc.get("$baseUrl/with-is-logged-in") {
                        with(user("tester"))
                    }

                Then("200 OK와 로그인 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].isLoggedIn") { value(true) }
                    }
                }
            }
        }

        Given("사용자 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyUserCreateRequest()

                every { service.save(any()) } returns userResponse

                val result =
                    mockMvc.post(baseUrl) {
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

            When("POST $baseUrl - 유효하지 않은 요청 (빈 필드)") {
                val invalidRequest =
                    dummyUserCreateRequest(
                        username = "",
                        password = "",
                        name = "",
                    )

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

        Given("사용자 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyUserUpdateRequest()

                every { service.update(1L, any()) } returns userResponse

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

            When("PATCH $baseUrl/{id} - 존재하지 않는 사용자") {
                val request = dummyUserUpdateRequest()

                every { service.update(999L, any()) } throws
                    EntityNotFoundException("사용자를 찾을 수 없습니다.")

                val result =
                    mockMvc.patch("$baseUrl/999") {
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

        Given("사용자 비밀번호 변경 API") {

            When("PATCH $baseUrl/{id}/password - 유효한 요청") {
                val request = dummyUserPasswordUpdateRequest()

                every { service.updateUserPassword(1L, any()) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1/password") {
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

        Given("사용자 역할 수정 API") {

            When("PATCH $baseUrl/{id}/roles - 유효한 요청") {
                val request = dummyUserRoleAssignRequest(roleIds = listOf(1L, 2L))

                every { service.updateUserRoles(1L, any()) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1/roles") {
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

        Given("사용자 비밀번호 초기화 API") {

            When("PATCH $baseUrl/{id}/password-init - 유효한 요청") {
                every { service.initPassword(1L) } just runs

                val result =
                    mockMvc.patch("$baseUrl/1/password-init") {
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

        Given("사용자 삭제 API") {

            When("DELETE $baseUrl/{id} - 존재하는 사용자") {
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

            When("DELETE $baseUrl/{id} - 존재하지 않는 사용자") {
                every { service.delete(999L) } throws
                    EntityNotFoundException("사용자를 찾을 수 없습니다.")

                val result =
                    mockMvc.delete("$baseUrl/999") {
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

        Given("사용자 역할 제거 API") {

            When("DELETE $baseUrl/{userId}/roles/{roleId} - 유효한 요청") {
                every { service.removeRoleFromUser(1L, 2L) } just runs

                val result =
                    mockMvc.delete("$baseUrl/1/roles/2") {
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
