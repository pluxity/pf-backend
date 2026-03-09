package com.pluxity.common.auth.permission

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.permission.dto.PermissionResponse
import com.pluxity.common.auth.permission.dto.ResourceTypeResponse
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.test.dto.dummyPermissionCreateRequest
import com.pluxity.common.test.dto.dummyPermissionUpdateRequest
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

@WebMvcTest(PermissionController::class)
class PermissionControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val permissionService: PermissionService,
) : BehaviorSpec({

        val baseUrl = "/permissions"

        val permissionResponse =
            PermissionResponse(
                id = 1L,
                name = "건물 읽기 권한",
                description = "건물 조회만 가능",
                resourcePermissions = emptyList(),
                domainPermissions = emptyList(),
                baseResponse =
                    BaseResponse(
                        createdAt = "2026-01-01T00:00:00",
                        createdBy = "system",
                        updatedAt = "2026-01-01T00:00:00",
                        updatedBy = "system",
                    ),
            )

        val resourceTypeResponse =
            ResourceTypeResponse(
                key = "BUILDING",
                name = "건물",
                endpoint = "/buildings",
                resources = emptyList(),
            )

        Given("권한 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyPermissionCreateRequest()

                every { permissionService.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 생성된 권한 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$") { value(1) }
                    }
                }
            }
        }

        Given("권한 목록 조회 API") {

            When("GET $baseUrl - 권한 목록 조회") {
                every { permissionService.findAll() } returns listOf(permissionResponse)

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 권한 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].id") { value(1) }
                        jsonPath("$.data[0].name") { value("건물 읽기 권한") }
                    }
                }
            }
        }

        Given("권한 상세 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 권한") {
                every { permissionService.findById(1L) } returns permissionResponse

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 권한 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.name") { value("건물 읽기 권한") }
                        jsonPath("$.data.description") { value("건물 조회만 가능") }
                        jsonPath("$.data.resourcePermissions") { isArray() }
                        jsonPath("$.data.domainPermissions") { isArray() }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 권한") {
                every { permissionService.findById(999L) } throws
                    CustomException(ErrorCode.NOT_FOUND_PERMISSION, 999L)

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

        Given("권한 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyPermissionUpdateRequest()

                every { permissionService.update(1L, any()) } just runs

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

        Given("권한 삭제 API") {

            When("DELETE $baseUrl/{id} - 존재하는 권한") {
                every { permissionService.delete(1L) } just runs

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

        Given("리소스 타입 목록 조회 API") {

            When("GET $baseUrl/resource-types - 리소스 타입 목록 조회") {
                every { permissionService.findAllResourceTypes() } returns listOf(resourceTypeResponse)

                val result =
                    mockMvc.get("$baseUrl/resource-types") {
                        with(user("tester"))
                    }

                Then("200 OK와 리소스 타입 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].key") { value("BUILDING") }
                        jsonPath("$.data[0].name") { value("건물") }
                        jsonPath("$.data[0].endpoint") { value("/buildings") }
                        jsonPath("$.data[0].resources") { isArray() }
                        jsonPath("$.data[0].resources.length()") { value(0) }
                    }
                }
            }
        }
    })
