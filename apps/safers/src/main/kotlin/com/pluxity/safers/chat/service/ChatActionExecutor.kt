package com.pluxity.safers.chat.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.service.CctvService
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.QueryTarget
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.event.service.EventService
import com.pluxity.safers.llm.dto.CctvFilterCriteria
import com.pluxity.safers.llm.dto.EventFilterCriteria
import com.pluxity.safers.site.dto.SiteResponse
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class ChatActionExecutor(
    private val eventService: EventService,
    private val cctvService: CctvService,
    private val weatherService: WeatherService,
    private val siteRepository: SiteRepository,
) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val MAX_PAGE_SIZE = 200
    }

    suspend fun execute(actions: List<QueryAction>): Map<String, Any> =
        coroutineScope {
            actions
                .map { action ->
                    async(Dispatchers.IO) {
                        try {
                            action.id to executeOne(action)
                        } catch (e: Exception) {
                            log.error(e) { "액션 실행 실패: ${action.id} (${action.target})" }
                            action.id to mapOf("error" to "데이터 조회에 실패했습니다")
                        }
                    }
                }.awaitAll()
                .toMap()
        }

    private fun executeOne(action: QueryAction): Any =
        when (action.target) {
            QueryTarget.EVENT -> executeEventQuery(action)
            QueryTarget.CCTV -> executeCctvQuery(action)
            QueryTarget.WEATHER -> executeWeatherQuery(action)
            QueryTarget.SITE -> executeSiteQuery(action)
        }

    private fun parseDateTime(value: String): LocalDateTime {
        val today = java.time.LocalDate.now()
        return when (value.lowercase().trim()) {
            "today" -> today.atStartOfDay()
            "yesterday" -> today.minusDays(1).atStartOfDay()
            "tomorrow" -> today.plusDays(1).atStartOfDay()
            else ->
                if (value.contains("T")) {
                    LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } else {
                    java.time.LocalDate
                        .parse(value)
                        .atStartOfDay()
                }
        }
    }

    private fun executeEventQuery(action: QueryAction): PageResponse<EventResponse> {
        val filters = action.filters
        val page = filters["page"]?.toString()?.toIntOrNull() ?: 1
        val size =
            (filters["size"]?.toString()?.toIntOrNull() ?: DEFAULT_PAGE_SIZE)
                .coerceAtMost(MAX_PAGE_SIZE)

        val criteria =
            EventFilterCriteria(
                startDate = filters["startDate"]?.toString()?.let { parseDateTime(it) },
                endDate = filters["endDate"]?.toString()?.let { parseDateTime(it) },
                types =
                    (filters["types"] as? List<*>)?.mapNotNull { typeName ->
                        runCatching { EventType.valueOf(typeName.toString()) }.getOrNull()
                    },
                siteId = filters["siteId"]?.toString()?.toLongOrNull(),
            )

        return eventService.findAll(
            PageSearchRequest(page = page, size = size),
            criteria,
        )
    }

    private fun executeCctvQuery(action: QueryAction): List<CctvResponse> {
        val filters = action.filters
        val criteria =
            CctvFilterCriteria(
                name = filters["name"] as? String,
                siteIds = filters["siteId"]?.let { listOf(it.toString().toLong()) },
            )
        return cctvService.findAll(criteria)
    }

    private fun executeWeatherQuery(action: QueryAction): Any {
        val siteId = action.filters["siteId"]?.toString()?.toLongOrNull()
        if (siteId != null) {
            return weatherService.findDashboard(siteId)
        }
        return siteRepository.findAll().associate { site ->
            site.name to weatherService.findDashboard(site.requiredId)
        }
    }

    private fun executeSiteQuery(action: QueryAction): List<SiteResponse> {
        val siteId = action.filters["siteId"]?.toString()?.toLongOrNull()
        if (siteId != null) {
            val site =
                siteRepository.findByIdOrNull(siteId)
                    ?: throw IllegalArgumentException("ID가 ${siteId}인 현장을 찾을 수 없습니다")
            return listOf(site.toResponse(null))
        }
        return siteRepository.findAll().map { it.toResponse(null) }
    }
}
