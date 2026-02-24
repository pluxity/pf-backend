package com.pluxity.safers.event.entity

import com.pluxity.common.core.test.withId
import java.time.LocalDateTime

fun dummyEvent(
    id: Long? = 1L,
    eventId: String = "EVT-20240101-001",
    eventTimestamp: LocalDateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0),
    category: EventCategory = EventCategory.DETECTION,
    type: EventType = EventType.NO_HELMET,
    trackId: Long = 12345L,
    name: String = "헬멧 미착용 감지",
    bbox: String? = "[100, 200, 300, 400]",
    centerX: Double? = 150.5,
    centerY: Double? = 200.0,
    confidence: Double? = 0.95,
    path: String = "",
    snapshotFileId: Long? = null,
    videoFileId: Long? = null,
): Event =
    Event(
        eventId = eventId,
        eventTimestamp = eventTimestamp,
        category = category,
        type = type,
        trackId = trackId,
        name = name,
        bbox = bbox,
        centerX = centerX,
        centerY = centerY,
        confidence = confidence,
        path = path,
    ).withId(id).apply {
        snapshotFileId?.let { assignSnapshotFile(it) }
        videoFileId?.let { assignVideoFile(it) }
    }
