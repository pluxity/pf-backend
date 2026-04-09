package com.pluxity.safers.chat.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.llm.dto.CctvFilterCriteria
import com.pluxity.safers.llm.dto.EventFilterCriteria
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "target")
sealed class ActionFilter {
    @JsonTypeName("EVENT")
    data class Event(
        val siteId: Long? = null,
        val types: List<EventType>? = null,
        val startDate: String? = null,
        val endDate: String? = null,
    ) : ActionFilter() {
        fun toCriteria() =
            EventFilterCriteria(
                startDate = startDate?.let { parseDateTime(it) },
                endDate = endDate?.let { parseDateTime(it) },
                types = types,
                siteId = siteId,
            )

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
    }

    @JsonTypeName("CCTV")
    data class Cctv(
        val name: String? = null,
        val siteId: Long? = null,
    ) : ActionFilter() {
        fun toCriteria(): CctvFilterCriteria =
            CctvFilterCriteria(
                name = name,
                siteIds = siteId?.let { listOf(it) },
            )
    }

    @JsonTypeName("WEATHER")
    data class Weather(
        val siteId: Long? = null,
    ) : ActionFilter()

    @JsonTypeName("SITE")
    data class Site(
        val siteId: Long? = null,
    ) : ActionFilter()
}
