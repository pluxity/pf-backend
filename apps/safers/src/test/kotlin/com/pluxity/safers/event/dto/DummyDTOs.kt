package com.pluxity.safers.event.dto

import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventType
import java.time.LocalDateTime

fun dummyEventCreateRequest(
    eventId: String = "EVT-20240101-001",
    timestamp: LocalDateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0),
    category: EventCategory = EventCategory.DETECTION,
    type: EventType = EventType.NO_HELMET,
    trackId: Long = 12345L,
    name: String = "헬멧 미착용 감지",
    snapshot: String = "snapshot_001.jpg",
    bbox: List<Int>? = listOf(100, 200, 300, 400),
    center: EventCreateRequest.Center? = EventCreateRequest.Center(150.5, 200.0),
    confidence: Double? = 0.95,
    path: String = "",
): EventCreateRequest =
    EventCreateRequest(
        eventId = eventId,
        timestamp = timestamp,
        category = category,
        type = type,
        trackId = trackId,
        name = name,
        snapshot = snapshot,
        bbox = bbox,
        center = center,
        confidence = confidence,
        path = path,
    )
