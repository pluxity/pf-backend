package com.pluxity.safers.cctv.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.dummyCctvPlaybackRequest
import com.pluxity.safers.cctv.dto.dummyCctvPlaybackResponse
import com.pluxity.safers.cctv.dto.dummyCctvResponse
import com.pluxity.safers.cctv.dto.dummyCctvUpdateRequest
import com.pluxity.safers.cctv.service.CctvFacade
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

@WebMvcTest(CctvController::class)
class CctvControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val cctvFacade: CctvFacade,
) : BehaviorSpec({

        val baseUrl = "/cctvs"

        Given("CCTV 동기화 API") {

            When("POST $baseUrl/sync - siteId 지정") {
                every { cctvFacade.sync(1L) } just runs

                val result =
                    mockMvc.post("$baseUrl/sync") {
                        param("siteId", "1")
                        with(csrf())
                        with(user("tester"))
                    }

                Then("204 No Content가 반환된다") {
                    result.andExpect {
                        status { isNoContent() }
                    }
                }
            }

            When("POST $baseUrl/sync - siteId 없이") {
                every { cctvFacade.sync(null) } just runs

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

            When("GET $baseUrl - siteId로 조회") {
                val responses = listOf(dummyCctvResponse())

                every { cctvFacade.findAll(1L) } returns responses

                val result =
                    mockMvc.get(baseUrl) {
                        param("siteId", "1")
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

            When("GET $baseUrl - CCTV가 없는 경우") {
                every { cctvFacade.findAll(null) } returns emptyList()

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

        Given("CCTV 수정 API") {

            When("PATCH $baseUrl/{id} - 유효한 요청") {
                val request = dummyCctvUpdateRequest()

                every { cctvFacade.update(1L, any()) } just runs

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

            When("PATCH $baseUrl/{id} - 존재하지 않는 CCTV") {
                val request = dummyCctvUpdateRequest()

                every { cctvFacade.update(999L, any()) } throws
                    CustomException(CctvErrorCode.NOT_FOUND_CCTV, 999L)

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

        Given("NVR 녹화영상 재생 세션 생성 API") {

            When("POST $baseUrl/{id}/playback - 유효한 요청") {
                val request = dummyCctvPlaybackRequest()

                every { cctvFacade.playback(1L, any()) } returns dummyCctvPlaybackResponse()

                val result =
                    mockMvc.post("$baseUrl/1/playback") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 재생 경로가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.pathName") { value("playback-pb_38ae550d") }
                    }
                }
            }

            When("POST $baseUrl/{id}/playback - 존재하지 않는 CCTV") {
                val request = dummyCctvPlaybackRequest()

                every { cctvFacade.playback(999L, any()) } throws
                    CustomException(CctvErrorCode.NOT_FOUND_CCTV, 999L)

                val result =
                    mockMvc.post("$baseUrl/999/playback") {
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

            When("POST $baseUrl/{id}/playback - NVR 정보 누락") {
                val request = dummyCctvPlaybackRequest()

                every { cctvFacade.playback(1L, any()) } throws
                    CustomException(CctvErrorCode.MISSING_NVR_INFO, 1L)

                val result =
                    mockMvc.post("$baseUrl/1/playback") {
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

        Given("NVR 녹화영상 재생 세션 삭제 API") {

            When("DELETE $baseUrl/{id}/playback/{pathName} - 존재하는 세션") {
                every { cctvFacade.deletePlayback(1L, "playback-pb_38ae550d") } just runs

                val result =
                    mockMvc.delete("$baseUrl/1/playback/playback-pb_38ae550d") {
                        with(csrf())
                        with(user("tester"))
                    }

                Then("204 No Content가 반환된다") {
                    result.andExpect {
                        status { isNoContent() }
                    }
                }
            }

            When("DELETE $baseUrl/{id}/playback/{pathName} - 존재하지 않는 CCTV") {
                every { cctvFacade.deletePlayback(999L, "some-path") } throws
                    CustomException(CctvErrorCode.NOT_FOUND_CCTV, 999L)

                val result =
                    mockMvc.delete("$baseUrl/999/playback/some-path") {
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
    })
