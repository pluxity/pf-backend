package com.pluxity.safers.event.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventType
import java.time.LocalDateTime

data class EventResponse(
    val id: Long,
    val eventId: String,
    val timestamp: LocalDateTime,
    val category: EventCategory,
    val type: EventType,
    val trackId: Long,
    val name: String,
    val confidence: Double?,
    val path: String,
    val snapshot: FileResponse?,
    val video: FileResponse?,
)

fun Event.toResponse(
    snapshotFileResponse: FileResponse?,
    videoFileResponse: FileResponse? = null,
): EventResponse =
    EventResponse(
        id = requiredId,
        eventId = eventId,
        timestamp = eventTimestamp,
        category = category,
        type = type,
        trackId = trackId,
        name = name,
        confidence = confidence,
        path = path,
        snapshot = snapshotFileResponse ?: FileResponse(),
        video = videoFileResponse ?: FileResponse(),
    )
