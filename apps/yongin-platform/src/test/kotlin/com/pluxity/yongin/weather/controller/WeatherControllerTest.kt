package com.pluxity.yongin.weather.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.yongin.weather.dto.dummyWeatherResponse
import com.pluxity.yongin.weather.service.WeatherService
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
import org.springframework.test.web.servlet.post

@WebMvcTest(WeatherController::class)
class WeatherControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val weatherService: WeatherService,
) : BehaviorSpec({

        Given("최신 날씨 조회 API") {

            When("GET /weather - 성공") {
                val response = dummyWeatherResponse()

                every { weatherService.findLatest() } returns response

                val result =
                    mockMvc.get("/weather") {
                        with(user("tester"))
                    }

                Then("200 OK와 날씨 정보가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data.id") { value(1) }
                        jsonPath("$.data.temperature") { value(15.0) }
                        jsonPath("$.data.humidity") { value(60.0) }
                    }
                }
            }
        }

        Given("날씨 웹훅 수신 API") {

            When("POST /weather/webhook - form data 수신") {
                every { weatherService.saveFromJson(any()) } just runs

                val result =
                    mockMvc.post("/weather/webhook") {
                        contentType = MediaType.APPLICATION_FORM_URLENCODED
                        param("type", "WEATHER")
                        param(
                            "data",
                            """{"measuredAt":"2026-03-06 12:00:00","temperature":15.5,"humidity":60.0,"windSpeed":3.5,"windDirection":"북서","rainfall":0,"pm10":35,"pm25":15,"pm10Status":"보통","pm25Status":"좋음","noise":45.0}""",
                        )
                        with(csrf())
                        with(user("tester"))
                    }

                Then("200 OK와 웹훅 응답이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.status") { value(0) }
                        jsonPath("$.msg") { value("성공") }
                    }
                }
            }
        }
    })
