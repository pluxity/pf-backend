package com.pluxity.safers.weather.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.safers.weather.dto.WeatherItemResponse
import com.pluxity.safers.weather.dto.WeatherTimeGroupResponse
import com.pluxity.safers.weather.entity.WeatherCategory
import com.pluxity.safers.weather.service.WeatherService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(WeatherController::class)
class WeatherControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val weatherService: WeatherService,
) : BehaviorSpec({

        val baseUrl = "/sites/1/weather"

        Given("날씨 대시보드 조회 API") {

            When("GET $baseUrl - 날씨 데이터 조회") {
                val responses =
                    listOf(
                        WeatherTimeGroupResponse(
                            fcstDate = "20260306",
                            fcstTime = "1300",
                            items =
                                listOf(
                                    WeatherItemResponse(
                                        category = WeatherCategory.T1H,
                                        value = "15.0",
                                    ),
                                ),
                        ),
                    )

                every { weatherService.findDashboard(1L) } returns responses

                val result =
                    mockMvc.get(baseUrl) {
                        with(user("tester"))
                    }

                Then("200 OK와 날씨 데이터가 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(1) }
                        jsonPath("$.data[0].fcstDate") { value("20260306") }
                        jsonPath("$.data[0].fcstTime") { value("1300") }
                        jsonPath("$.data[0].items[0].value") { value("15.0") }
                    }
                }
            }

            When("GET $baseUrl - 날씨 데이터가 없는 경우") {
                every { weatherService.findDashboard(1L) } returns emptyList()

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
    })
