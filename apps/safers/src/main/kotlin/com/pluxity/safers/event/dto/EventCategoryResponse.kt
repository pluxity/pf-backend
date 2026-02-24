package com.pluxity.safers.event.dto

import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventType

data class EventCategoryResponse(
    val name: String,
    val displayName: String,
    val types: List<EventTypeResponse>,
)

fun EventCategory.toResponse(): EventCategoryResponse =
    EventCategoryResponse(
        name = name,
        displayName = displayName,
        types = EventType.entries.filter { it.category == this }.map { it.toResponse() },
    )
