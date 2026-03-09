package com.pluxity.yongin.announcement.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.yongin.announcement.dto.dummyAnnouncementRequest
import com.pluxity.yongin.announcement.dto.dummyAnnouncementResponse
import com.pluxity.yongin.announcement.service.AnnouncementService
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

@WebMvcTest(AnnouncementController::class)
class AnnouncementControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val service: AnnouncementService,
) : BehaviorSpec({

        Given("안내사항 조회 API") {

            When("GET /announcement - 성공") {
                val response = dummyAnnouncementResponse()

                every { service.getAnnouncement() } returns response

                val result =
                    mockMvc.get("/announcement") {
                        with(user("tester"))
                    }

                Then("200 OK와 안내사항이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { value("테스트 안내사항") }
                    }
                }
            }
        }

        Given("안내사항 수정 API") {

            When("PUT /announcement - 유효한 요청") {
                val request = dummyAnnouncementRequest()

                every { service.saveAnnouncement(any()) } just runs

                val result =
                    mockMvc.put("/announcement") {
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

            When("PUT /announcement - 필수 필드 누락") {
                val invalidRequest = dummyAnnouncementRequest(content = "")

                val result =
                    mockMvc.put("/announcement") {
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
    })
