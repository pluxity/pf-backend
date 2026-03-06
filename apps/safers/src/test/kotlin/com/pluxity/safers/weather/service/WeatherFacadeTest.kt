package com.pluxity.safers.weather.service

import com.pluxity.safers.site.entity.dummySite
import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.weather.client.WeatherApiClient
import com.pluxity.safers.weather.dto.dummyForecastApiResponse
import com.pluxity.safers.weather.dto.dummyObservationApiResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono

class WeatherFacadeTest :
    BehaviorSpec({

        val weatherService: WeatherService = mockk(relaxed = true)
        val weatherApiClient: WeatherApiClient = mockk()
        val siteRepository: SiteRepository = mockk()

        val facade = WeatherFacade(weatherService, weatherApiClient, siteRepository)

        Given("초단기 예보 수집") {

            When("등록된 현장이 있으면") {
                val site = dummySite(id = 1L, nx = 55, ny = 127)

                every { siteRepository.findAll() } returns listOf(site)
                every {
                    weatherApiClient.fetchUltraSrtFcst(any(), any(), 55, 127)
                } returns Mono.just(dummyForecastApiResponse())

                facade.collectForecast()

                Then("예보 데이터 저장이 호출된다") {
                    verify { weatherService.saveForecastData(any()) }
                }
            }

            When("등록된 현장이 없으면") {
                every { siteRepository.findAll() } returns emptyList()

                facade.collectForecast()

                Then("API 호출이 수행되지 않는다") {
                    verify(exactly = 0) { weatherApiClient.fetchUltraSrtFcst(any(), any(), any(), any()) }
                }
            }

            When("nx가 0인 현장은 제외된다") {
                val site = dummySite(id = 1L, nx = 0, ny = 0)

                every { siteRepository.findAll() } returns listOf(site)

                facade.collectForecast()

                Then("API 호출이 수행되지 않는다") {
                    verify(exactly = 0) { weatherApiClient.fetchUltraSrtFcst(any(), any(), any(), any()) }
                }
            }
        }

        Given("초단기 실황 수집") {

            When("등록된 현장이 있으면") {
                val site = dummySite(id = 1L, nx = 55, ny = 127)

                every { siteRepository.findAll() } returns listOf(site)
                every {
                    weatherApiClient.fetchUltraSrtNcst(any(), any(), 55, 127)
                } returns Mono.just(dummyObservationApiResponse())

                facade.collectObservation()

                Then("실황 데이터 저장이 호출된다") {
                    verify { weatherService.saveObservationData(any()) }
                }
            }

            When("등록된 현장이 없으면") {
                every { siteRepository.findAll() } returns emptyList()

                facade.collectObservation()

                Then("API 호출이 수행되지 않는다") {
                    verify(exactly = 0) { weatherApiClient.fetchUltraSrtNcst(any(), any(), any(), any()) }
                }
            }
        }
    })
