package com.pluxity.safers.chat.service

import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.cctv.service.CctvService
import com.pluxity.safers.chat.dto.ActionFilter
import com.pluxity.safers.chat.dto.ActionResult
import com.pluxity.safers.chat.dto.ChatWeatherResponse
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.QueryTarget
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.service.EventService
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.site.dto.SiteSummary
import com.pluxity.safers.site.entity.dummySite
import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.site.service.SiteService
import com.pluxity.safers.weather.service.WeatherService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull

class ChatActionExecutorTest :
    BehaviorSpec({

        val eventService: EventService = mockk(relaxed = true)
        val cctvService: CctvService = mockk(relaxed = true)
        val weatherService: WeatherService = mockk(relaxed = true)
        val siteService: SiteService = mockk(relaxed = true)
        val siteRepository: SiteRepository = mockk(relaxed = true)
        val executor =
            ChatActionExecutor(
                eventService = eventService,
                cctvService = cctvService,
                weatherService = weatherService,
                siteService = siteService,
                siteRepository = siteRepository,
            )

        beforeContainer { clearAllMocks(answers = false) }

        Given("EVENT 액션 정상 실행") {

            When("이벤트 조회가 성공하면") {
                val action =
                    QueryAction(
                        id = "evt_1",
                        target = QueryTarget.EVENT,
                        filters = ActionFilter.Event(siteId = 1L),
                    )
                every { eventService.findAll(any(), any()) } returns
                    PageResponse<EventResponse>(
                        content = emptyList(),
                        pageNumber = 1,
                        pageSize = 50,
                        totalElements = 0,
                        last = true,
                        first = true,
                    )

                val result = executor.execute(listOf(action))

                Then("PaginatedEvent 결과가 반환된다") {
                    result shouldContainKey "evt_1"
                    result["evt_1"].shouldBeInstanceOf<ActionResult.PaginatedEvent>()
                }
            }
        }

        Given("WEATHER 액션 - 존재하지 않는 현장") {

            When("siteId가 사이트 목록에 없으면") {
                val action =
                    QueryAction(
                        id = "wx_1",
                        target = QueryTarget.WEATHER,
                        filters = ActionFilter.Weather(siteId = 999L),
                    )
                every { siteService.findAllSites() } returns
                    listOf(SiteSummary(id = 1L, name = "서울역 현장"))

                val result = executor.execute(listOf(action))

                Then("Failure로 변환되고 errorCode가 NOT_FOUND_SITE이다") {
                    val failure = result["wx_1"].shouldBeInstanceOf<ActionResult.Failure>()
                    failure.actionId shouldBe "wx_1"
                    failure.target shouldBe QueryTarget.WEATHER
                    failure.errorCode shouldBe SafersErrorCode.NOT_FOUND_SITE.name
                    failure.message shouldContain "999"
                }
            }
        }

        Given("WEATHER 액션 - 정상") {

            When("siteId가 존재하면") {
                val action =
                    QueryAction(
                        id = "wx_2",
                        target = QueryTarget.WEATHER,
                        filters = ActionFilter.Weather(siteId = 1L),
                    )
                every { siteService.findAllSites() } returns
                    listOf(SiteSummary(id = 1L, name = "서울역 현장"))
                every { weatherService.findDashboard(1L) } returns emptyList()

                val result = executor.execute(listOf(action))

                Then("SingleResult로 ChatWeatherResponse가 반환된다") {
                    val single = result["wx_2"].shouldBeInstanceOf<ActionResult.SingleResult>()
                    val payload = single.data.shouldBeInstanceOf<ChatWeatherResponse>()
                    payload.siteName shouldBe "서울역 현장"
                }
            }
        }

        Given("SITE 액션 - 존재하지 않는 ID") {

            When("siteRepository에서 null을 반환하면") {
                val action =
                    QueryAction(
                        id = "site_1",
                        target = QueryTarget.SITE,
                        filters = ActionFilter.Site(siteId = 999L),
                    )
                every { siteRepository.findByIdOrNull(999L) } returns null

                val result = executor.execute(listOf(action))

                Then("Failure로 변환되고 도메인 errorCode가 보존된다") {
                    val failure = result["site_1"].shouldBeInstanceOf<ActionResult.Failure>()
                    failure.errorCode shouldBe SafersErrorCode.NOT_FOUND_SITE.name
                }
            }
        }

        Given("SITE 액션 - 전체 목록") {

            When("siteId가 null이면") {
                val action =
                    QueryAction(
                        id = "site_all",
                        target = QueryTarget.SITE,
                        filters = ActionFilter.Site(siteId = null),
                    )
                every { siteRepository.findAll() } returns listOf(dummySite(id = 1L))

                val result = executor.execute(listOf(action))

                Then("ListResult가 반환된다") {
                    result["site_all"].shouldBeInstanceOf<ActionResult.ListResult<*>>()
                }
            }
        }

        Given("CCTV 액션 - 내부 예외 발생") {

            When("cctvService가 예상치 못한 RuntimeException을 던지면") {
                val action =
                    QueryAction(
                        id = "cctv_1",
                        target = QueryTarget.CCTV,
                        filters = ActionFilter.Cctv(siteId = 1L),
                    )
                every { cctvService.findAll(any()) } throws RuntimeException("DB connection lost")

                val result = executor.execute(listOf(action))

                Then("INTERNAL Failure로 변환된다") {
                    val failure = result["cctv_1"].shouldBeInstanceOf<ActionResult.Failure>()
                    failure.errorCode shouldBe "INTERNAL"
                    failure.target shouldBe QueryTarget.CCTV
                }
            }
        }

        Given("여러 액션을 동시에 실행할 때 일부만 실패") {

            When("정상 액션과 실패 액션이 함께 들어오면") {
                val ok =
                    QueryAction(
                        id = "wx_ok",
                        target = QueryTarget.WEATHER,
                        filters = ActionFilter.Weather(siteId = 1L),
                    )
                val fail =
                    QueryAction(
                        id = "wx_fail",
                        target = QueryTarget.WEATHER,
                        filters = ActionFilter.Weather(siteId = 999L),
                    )
                every { siteService.findAllSites() } returns
                    listOf(SiteSummary(id = 1L, name = "서울역 현장"))
                every { weatherService.findDashboard(1L) } returns emptyList()

                val result = executor.execute(listOf(ok, fail))

                Then("성공 결과와 실패 결과가 모두 반환된다") {
                    result["wx_ok"].shouldBeInstanceOf<ActionResult.SingleResult>()
                    val failure = result["wx_fail"].shouldBeInstanceOf<ActionResult.Failure>()
                    failure.errorCode shouldBe SafersErrorCode.NOT_FOUND_SITE.name
                }
            }
        }
    })
