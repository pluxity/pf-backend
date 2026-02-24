package com.pluxity.safers.event.dto

import com.pluxity.safers.event.entity.EventType

data class EventTypeResponse(
    val name: String,
    val displayName: String,
)

fun EventType.toResponse(): EventTypeResponse =
    EventTypeResponse(
        name = name,
        displayName = displayName,
    )
