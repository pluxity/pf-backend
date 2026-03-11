package com.pluxity.safers.event.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.core.aop.ResponseCreatedAspect
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.dto.dummyEventCreateRequest
import com.pluxity.safers.event.dto.dummyEventResponse
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.service.EventFacade
import com.pluxity.safers.global.constant.SafersErrorCode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(EventController::class)
@Import(ResponseCreatedAspect::class)
@EnableAspectJAutoProxy
class EventControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val eventFacade: EventFacade,
) : BehaviorSpec({

        val baseUrl = "/events"

        Given("이벤트 생성 API") {

            When("POST $baseUrl - 유효한 요청") {
                val request = dummyEventCreateRequest()

                every { eventFacade.create(any()) } returns 1L

                val result =
                    mockMvc.post(baseUrl) {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("201 Created가 반환된다") {
                    result.andExpect {
                        status { isCreated() }
                        header { string("Location", "/events/1") }
                    }
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

        Given("이벤트 단건 조회 API") {

            When("GET $baseUrl/{id} - 존재하는 이벤트") {
                val response = dummyEventResponse()

                every { eventFacade.findById(1L) } returns response

                val result =
                    mockMvc.get("$baseUrl/1") {
                        with(user("tester"))
                    }

                Then("200 OK와 이벤트 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.eventId") { value("EVT-20260101-001") }
                        jsonPath("$.data.name") { value("헬멧 미착용 감지") }
                    }
                }
            }

            When("GET $baseUrl/{id} - 존재하지 않는 이벤트") {
                every { eventFacade.findById(999L) } throws
                    CustomException(SafersErrorCode.NOT_FOUND_EVENT, 999L)

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

        Given("이벤트 목록 조회 API") {

            When("GET $baseUrl - 페이징 조회") {
                val pageResponse =
                    PageResponse(
                        content = listOf(dummyEventResponse()),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 1,
                        last = true,
                        first = true,
                    )

                every { eventFacade.findAll(any(), any(), any()) } returns pageResponse

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

            When("GET $baseUrl - 날짜 필터링 조회") {
                val pageResponse =
                    PageResponse(
                        content = emptyList<EventResponse>(),
                        pageNumber = 1,
                        pageSize = 10,
                        totalElements = 0,
                        last = true,
                        first = true,
                    )

                every { eventFacade.findAll(any(), any(), any()) } returns pageResponse

                val result =
                    mockMvc.get(baseUrl) {
                        param("page", "1")
                        param("size", "10")
                        param("startDate", "20260101000000")
                        param("endDate", "20261231235959")
                        with(user("tester"))
                    }

                Then("200 OK와 필터링된 결과가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.content") { isArray() }
                        jsonPath("$.data.totalElements") { value(0) }
                    }
                }
            }
        }

        Given("이벤트 영상 등록 API") {

            When("POST $baseUrl/{eventId}/video - 유효한 요청") {
                val request = mapOf("video" to "http://localhost:8080/videos/clip.mp4")

                every { eventFacade.uploadVideo(1L, any()) } just runs

                val result =
                    mockMvc.post("$baseUrl/1/video") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                    }
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

        Given("이벤트 카테고리 목록 조회 API") {

            When("GET $baseUrl/categories") {
                val result =
                    mockMvc.get("$baseUrl/categories") {
                        with(user("tester"))
                    }

                Then("200 OK와 카테고리 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(EventCategory.entries.size) }
                    }
                }
            }
        }
    })
