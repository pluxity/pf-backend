package com.pluxity.safers.collect.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.event.dto.dummyEventCreateRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(CctvEventCollectController::class)
class CctvEventCollectControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val cctvEventCollector: CctvEventCollector,
) : BehaviorSpec({

        val baseUrl = "/collect/events"

        Given("이벤트 수집 API (Kafka)") {

            When("POST $baseUrl - 유효한 요청") {
                every { cctvEventCollector.collect(any()) } just runs

                val request = dummyEventCreateRequest()
                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("202 Accepted가 반환된다") {
                    result.andExpect {
                        status { isAccepted() }
                    }
                }

                Then("CctvEventCollector.collect가 호출된다") {
                    verify { cctvEventCollector.collect(any()) }
                }
            }

            When("POST $baseUrl - 필수 필드 누락") {
                val invalidRequest = dummyEventCreateRequest(eventId = "", name = "", snapshot = "")

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

        Given("영상 수집 API (Kafka)") {

            When("POST $baseUrl/{eventId}/video - 유효한 요청") {
                every { cctvEventCollector.collectVideo(any(), any()) } just runs

                val request = mapOf("video" to "http://localhost:8080/videos/clip.mp4")
                val result =
                    mockMvc.post("$baseUrl/1/video") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("202 Accepted가 반환된다") {
                    result.andExpect {
                        status { isAccepted() }
                    }
                }

                Then("CctvEventCollector.collectVideo가 호출된다") {
                    verify { cctvEventCollector.collectVideo(eq(1L), any()) }
                }
            }

            When("POST $baseUrl/{eventId}/video - 영상 URL 누락") {
                val invalidRequest = mapOf("video" to "")

                val result =
                    mockMvc.post("$baseUrl/1/video") {
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
