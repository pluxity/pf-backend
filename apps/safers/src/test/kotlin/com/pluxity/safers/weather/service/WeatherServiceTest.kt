package com.pluxity.safers.weather.service

import com.pluxity.safers.site.entity.dummySite
import com.pluxity.safers.weather.dto.dummyErrorApiResponse
import com.pluxity.safers.weather.dto.dummyForecastApiResponse
import com.pluxity.safers.weather.dto.dummyObservationApiResponse
import com.pluxity.safers.weather.entity.WeatherCategory
import com.pluxity.safers.weather.entity.dummyWeather
import com.pluxity.safers.weather.repository.WeatherRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class WeatherServiceTest :
    BehaviorSpec({

        val weatherRepository: WeatherRepository = mockk(relaxed = true)
        every { weatherRepository.save(any()) } answers { firstArg() }
        val service = WeatherService(weatherRepository)

        beforeContainer {
            clearMocks(weatherRepository, answers = false)
        }

        Given("대시보드 날씨 조회") {

            When("현장 ID로 날씨를 조회하면") {
                val weathers =
                    listOf(
                        dummyWeather(
                            id = 1L,
                            category = WeatherCategory.T1H,
                            fcstDate = "20260306",
                            fcstTime = "1300",
                            fcstValue = "15.0",
                        ),
                        dummyWeather(
                            id = 2L,
                            category = WeatherCategory.REH,
                            fcstDate = "20260306",
                            fcstTime = "1300",
                            fcstValue = "60",
                        ),
                        dummyWeather(
                            id = 3L,
                            category = WeatherCategory.T1H,
                            fcstDate = "20260306",
                            fcstTime = "1400",
                            fcstValue = "16.0",
                        ),
                    )

                every {
                    weatherRepository.findBySiteIdAndFcstDateTimeBetween(1L, any(), any())
                } returns weathers

                val result = service.findDashboard(1L)

                Then("시간별로 그룹핑된 날씨 데이터가 반환된다") {
                    result shouldHaveSize 2
                    result[0].fcstDate shouldBe "20260306"
                    result[0].fcstTime shouldBe "1300"
                    result[0].items shouldHaveSize 2
                    result[1].fcstTime shouldBe "1400"
                    result[1].items shouldHaveSize 1
                }
            }

            When("날씨 데이터가 없으면") {
                every {
                    weatherRepository.findBySiteIdAndFcstDateTimeBetween(999L, any(), any())
                } returns emptyList()

                val result = service.findDashboard(999L)

                Then("빈 목록이 반환된다") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("초단기 예보 데이터 저장") {

            When("정상 응답이면 새 데이터를 저장한다") {
                val site = dummySite(id = 1L)
                val response = dummyForecastApiResponse()

                every { weatherRepository.findBySiteAndFcstDateIn(site, any()) } returns emptyList()

                service.saveForecastData(listOf(site to response))

                Then("날씨 데이터가 저장된다") {
                    verify { weatherRepository.save(any()) }
                }
            }

            When("기존 데이터가 있으면 업데이트한다") {
                val site = dummySite(id = 1L)
                val existing =
                    dummyWeather(
                        id = 1L,
                        site = site,
                        category = WeatherCategory.T1H,
                        fcstDate = "20260306",
                        fcstTime = "1300",
                        fcstValue = "10.0",
                    )
                val response = dummyForecastApiResponse()

                every { weatherRepository.findBySiteAndFcstDateIn(site, any()) } returns listOf(existing)

                service.saveForecastData(listOf(site to response))

                Then("기존 데이터가 업데이트된다") {
                    existing.fcstValue shouldBe "15.0"
                    verify(exactly = 0) { weatherRepository.save(any()) }
                }
            }

            When("API 응답 코드가 에러면") {
                val site = dummySite(id = 1L)
                val response = dummyErrorApiResponse()

                service.saveForecastData(listOf(site to response))

                Then("데이터를 저장하지 않는다") {
                    verify(exactly = 0) { weatherRepository.save(any()) }
                }
            }
        }

        Given("초단기 실황 데이터 저장") {

            When("정상 응답이면 새 데이터를 저장한다") {
                val site = dummySite(id = 1L)
                val response = dummyObservationApiResponse()

                every { weatherRepository.findBySiteAndFcstDateIn(site, any()) } returns emptyList()

                service.saveObservationData(listOf(site to response))

                Then("실황 데이터가 저장된다") {
                    verify { weatherRepository.save(any()) }
                }
            }

            When("기존 데이터가 있으면 업데이트한다") {
                val site = dummySite(id = 1L)
                val existing =
                    dummyWeather(
                        id = 1L,
                        site = site,
                        category = WeatherCategory.T1H,
                        fcstDate = "20260306",
                        fcstTime = "1200",
                        fcstValue = "10.0",
                    )
                val response = dummyObservationApiResponse()

                every { weatherRepository.findBySiteAndFcstDateIn(site, any()) } returns listOf(existing)

                service.saveObservationData(listOf(site to response))

                Then("기존 데이터가 업데이트된다") {
                    existing.fcstValue shouldBe "14.5"
                    verify(exactly = 0) { weatherRepository.save(any()) }
                }
            }
        }
    })
