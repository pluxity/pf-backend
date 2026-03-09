package com.pluxity.safers.site.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.site.dto.dummySiteRequest
import com.pluxity.safers.site.dto.dummySiteResponse
import com.pluxity.safers.site.entity.Region
import com.pluxity.safers.site.service.SiteService
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@WebMvcTest(SiteController::class)
class SiteControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val siteService: SiteService,
) : BehaviorSpec({

        val baseUrl = "/sites"

        Given("현장 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummySiteRequest()

                every { siteService.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 ID가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$") { value(1) }
                    }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest =
                    mapOf(
                        "name" to "",
                        "location" to "",
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

        Given("현장 단건 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 현장") {
                val response = dummySiteResponse()

                every { siteService.findById(1L) } returns response

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 현장 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.name") { value("서울역 현장") }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 현장") {
                every { siteService.findById(999L) } throws
                    CustomException(SafersErrorCode.NOT_FOUND_SITE, 999L)

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

        Given("현장 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummySiteResponse()),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 1,
                        last = true,
                        first = true,
                    )

                every { siteService.findAll(any()) } returns pageResponse

                val result =
                    mockMvc.get(baseUrl) {
                        param("page", "1")
                        param("size", "10")
                        with(user("tester"))
                    }

                Then("200 OK와 페이징 결과가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.totalElements") { value(1) }
                    }
                }
            }
        }

        Given("현장 수정 API") {

            When("PUT $baseUrl/{id} - 유효한 요청") {
                val request = dummySiteRequest(name = "수정된 현장")

                every { siteService.update(1L, any()) } just runs

                val result =
                    mockMvc.put("$baseUrl/1") {
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

            When("PUT $baseUrl/{id} - 존재하지 않는 현장") {
                val request = dummySiteRequest()

                every { siteService.update(999L, any()) } throws
                    CustomException(SafersErrorCode.NOT_FOUND_SITE, 999L)

                val result =
                    mockMvc.put("$baseUrl/999") {
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

        Given("현장 삭제 API") {

            When("DELETE $baseUrl/{id} - 존재하는 현장") {
                every { siteService.delete(1L) } just runs

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

        Given("지역 목록 조회 API") {

            When("GET $baseUrl/regions") {
                val result =
                    mockMvc.get("$baseUrl/regions") {
                        with(user("tester"))
                    }

                Then("200 OK와 지역 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(Region.entries.size) }
                    }
                }
            }
        }
    })
