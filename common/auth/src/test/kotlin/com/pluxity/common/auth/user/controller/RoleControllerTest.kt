package com.pluxity.common.auth.user.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.user.dto.RoleResponse
import com.pluxity.common.auth.user.service.RoleService
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.test.dto.dummyRoleCreateRequest
import com.pluxity.common.test.dto.dummyRoleUpdateRequest
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(RoleController::class)
class RoleControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val roleService: RoleService,
) : BehaviorSpec({

        val baseUrl = "/roles"

        val roleResponse =
            RoleResponse(
                id = 1L,
                name = "일반 사용자",
                description = "기본 역할",
                permissions = emptyList(),
            )

        Given("역할 상세 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 역할") {
                every { roleService.findById(1L) } returns roleResponse

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 역할 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.name") { value("일반 사용자") }
                        jsonPath("$.data.description") { value("기본 역할") }
                        jsonPath("$.data.permissions") { isArray() }
                        jsonPath("$.data.permissions.length()") { value(0) }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 역할") {
                every { roleService.findById(999L) } throws
                    CustomException(ErrorCode.NOT_FOUND_ROLE, 999L)

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

        Given("역할 목록 조회 API") {

            When("GET $baseUrl - 역할 목록 조회") {
                every { roleService.findAll() } returns listOf(roleResponse)

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 역할 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("일반 사용자") }
                    }
                }
            }
        }

        Given("역할 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyRoleCreateRequest()

                every { roleService.save(any(), any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 생성된 역할 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$") { value(1) }
                    }
                }
            }

            When("POST $baseUrl - name이 빈 문자열인 경우") {
                val request = dummyRoleCreateRequest(name = "")

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

        Given("역할 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyRoleUpdateRequest()

                every { roleService.update(1L, any()) } just runs

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

            When("PATCH $baseUrl/{id} - 존재하지 않는 역할") {
                val request = dummyRoleUpdateRequest()

                every { roleService.update(999L, any()) } throws
                    CustomException(ErrorCode.NOT_FOUND_ROLE, 999L)

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

        Given("역할 삭제 API") {

            When("DELETE $baseUrl/{id} - 존재하는 역할") {
                every { roleService.delete(1L) } just runs

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
