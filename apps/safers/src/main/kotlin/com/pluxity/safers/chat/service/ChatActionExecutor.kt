package com.pluxity.safers.chat.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.cctv.service.CctvService
import com.pluxity.safers.chat.dto.ActionFilter
import com.pluxity.safers.chat.dto.ActionResult
import com.pluxity.safers.chat.dto.ChatActionRequest
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.QueryContext
import com.pluxity.safers.chat.dto.QueryTarget
import com.pluxity.safers.chat.dto.toPaginatedEvent
import com.pluxity.safers.event.service.EventService
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.site.dto.toResponse
import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.weather.service.WeatherService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ChatActionExecutor(
    private val eventService: EventService,
    private val cctvService: CctvService,
    private val weatherService: WeatherService,
    private val siteRepository: SiteRepository,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 200
    }

    suspend fun execute(actions: List<QueryAction>): Map<String, ActionResult> =
        coroutineScope {
            actions
                .map { action ->
                    async(Dispatchers.IO) {
                        try {
                            action.id to executeOne(action)
                        } catch (e: Exception) {
                            log.error(e) { "액션 실행 실패: ${action.id} (${action.target})" }
                            action.id to ActionResult.SingleResult(data = mapOf("error" to "데이터 조회에 실패했습니다"))
                        }
                    }
                }.awaitAll()
                .toMap()
        }

    fun executeAction(request: ChatActionRequest): ActionResult {
        val action =
            QueryAction(
                id = request.actionId,
                target = request.target,
                filters = request.filters,
                page = request.page,
                size = request.size,
            )
        return executeOne(action)
    }

    private fun executeOne(action: QueryAction): ActionResult =
        when (action.target) {
            QueryTarget.EVENT -> executeEventQuery(action)
            QueryTarget.CCTV -> executeCctvQuery(action)
            QueryTarget.WEATHER -> executeWeatherQuery(action)
            QueryTarget.SITE -> executeSiteQuery(action)
        }

    private fun executeEventQuery(action: QueryAction): ActionResult.PaginatedEvent {
        val filter = action.filters as ActionFilter.Event
        val size = action.size.coerceAtMost(MAX_PAGE_SIZE)

        val pageResponse = eventService.findAll(PageSearchRequest(page = action.page, size = size), filter.toCriteria())

        return pageResponse.toPaginatedEvent(
            QueryContext(actionId = action.id, target = action.target, filters = filter),
        )
    }

    private fun executeCctvQuery(action: QueryAction): ActionResult.ListResult<*> {
        val filter = action.filters as ActionFilter.Cctv
        return ActionResult.ListResult(data = cctvService.findAll(filter.toCriteria()))
    }

    private fun executeWeatherQuery(action: QueryAction): ActionResult {
        val filter = action.filters as ActionFilter.Weather
        if (filter.siteId != null) {
            return ActionResult.SingleResult(data = weatherService.findDashboard(filter.siteId))
        }
        return ActionResult.SingleResult(
            data =
                siteRepository.findAll().associate { site ->
                    site.name to weatherService.findDashboard(site.requiredId)
                },
        )
    }

    private fun executeSiteQuery(action: QueryAction): ActionResult.ListResult<*> {
        val filter = action.filters as ActionFilter.Site
        if (filter.siteId != null) {
            val site =
                siteRepository.findByIdOrNull(filter.siteId)
                    ?: throw CustomException(SafersErrorCode.NOT_FOUND_SITE, filter.siteId)
            return ActionResult.ListResult(data = listOf(site.toResponse(null)))
        }
        return ActionResult.ListResult(data = siteRepository.findAll().map { it.toResponse(null) })
    }
}
