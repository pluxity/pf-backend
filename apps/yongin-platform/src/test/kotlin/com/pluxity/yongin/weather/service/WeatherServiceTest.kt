package com.pluxity.yongin.weather.service

import com.pluxity.yongin.weather.dto.WeatherDataDto
import com.pluxity.yongin.weather.dto.dummyWeatherDataDto
import com.pluxity.yongin.weather.entity.dummyWeather
import com.pluxity.yongin.weather.repository.WeatherRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.databind.ObjectMapper

class WeatherServiceTest :
    BehaviorSpec({

        val repository: WeatherRepository = mockk(relaxed = true)
        every { repository.save(any()) } answers { firstArg() }
        val objectMapper: ObjectMapper = mockk()
        val service = WeatherService(repository, objectMapper)

        Given("날씨 JSON 데이터 저장") {

            When("유효한 JSON을 저장하면") {
                val json = "test-json"
                val dto = dummyWeatherDataDto()

                every { objectMapper.readValue(json, WeatherDataDto::class.java) } returns dto

                service.saveFromJson(json)

                Then("날씨 데이터가 저장된다") {
                    verify {
                        repository.save(
                            match {
                                it.measuredAt == dto.measuredAt &&
                                    it.temperature == dto.temperature &&
                                    it.humidity == dto.humidity &&
                                    it.windSpeed == dto.windSpeed &&
                                    it.windDirection == dto.windDirection &&
                                    it.rainfall == dto.rainfall &&
                                    it.pm10 == dto.pm10 &&
                                    it.pm25 == dto.pm25 &&
                                    it.pm10Status == dto.pm10Status &&
                                    it.pm25Status == dto.pm25Status &&
                                    it.noise == dto.noise
                            },
                        )
                    }
                }
            }
        }

        Given("최신 날씨 조회") {

            When("날씨 데이터가 있으면") {
                val weather = dummyWeather(id = 1L)

                every { repository.findTopByOrderByIdDesc() } returns weather

                val result = service.findLatest()

                Then("최신 날씨 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.measuredAt shouldBe "2026-03-06 12:00:00"
                    result.temperature shouldBe 15.5
                    result.humidity shouldBe 60.0
                    result.windSpeed shouldBe 3.5
                    result.windDirection shouldBe "북서"
                    result.rainfall shouldBe 0
                    result.pm10 shouldBe 35
                    result.pm25 shouldBe 15
                    result.pm10Status shouldBe "보통"
                    result.pm25Status shouldBe "좋음"
                    result.noise shouldBe 45.0
                }
            }

            When("날씨 데이터가 없으면") {
                every { repository.findTopByOrderByIdDesc() } returns null

                val result = service.findLatest()

                Then("기본 빈 응답이 반환된다") {
                    result.id.shouldBeNull()
                    result.measuredAt.shouldBeNull()
                    result.temperature.shouldBeNull()
                }
            }
        }
    })
