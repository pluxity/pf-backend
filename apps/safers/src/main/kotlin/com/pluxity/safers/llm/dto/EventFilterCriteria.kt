package com.pluxity.safers.llm.dto

import com.pluxity.safers.event.entity.EventType
import java.time.LocalDateTime

data class EventFilterCriteria(
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val types: List<EventType>? = null,
)
