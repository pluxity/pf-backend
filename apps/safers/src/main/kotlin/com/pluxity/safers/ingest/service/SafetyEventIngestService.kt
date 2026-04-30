package com.pluxity.safers.ingest.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.listener.SafetyEventCreated
import com.pluxity.safers.event.repository.EventRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.ingest.dto.EventIngestRequest
import com.pluxity.safers.ingest.dto.SafetyEventResponse
import com.pluxity.safers.ingest.dto.toSafetyResponse
import com.pluxity.safers.ingest.repository.DeviceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SafetyEventIngestService(
    private val eventRepository: EventRepository,
    private val deviceRepository: DeviceRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun ingest(
        siteId: Long,
        request: EventIngestRequest,
    ): SafetyEventResponse {
        if (request.eventType.category != EventCategory.SAFETY) {
            throw CustomException(SafersErrorCode.INVALID_EVENT_TYPE, request.eventType.name)
        }

        val event =
            Event(
                eventId = request.eventId,
                eventTimestamp = request.occurredAt,
                category = EventCategory.SAFETY,
                type = request.eventType,
                name = request.eventType.displayName,
                siteId = siteId,
                severity = request.severity,
                source = request.source,
                deviceId = request.deviceId,
                bandId = request.bandId,
                lat = request.rawPosition?.lat,
                lon = request.rawPosition?.lon,
                alt = request.rawPosition?.alt,
                accuracyM = request.rawPosition?.accuracyM,
                payload = request.payload,
            )
        val saved =
            try {
                eventRepository.save(event)
            } catch (_: DataIntegrityViolationException) {
                // uk_events_event_id 위반 — 동일 eventId 중복 (재시도 또는 동시 요청)
                throw CustomException(SafersErrorCode.DUPLICATE_EVENT, request.eventId)
            }

        touchDeviceLastSeen(siteId, request.deviceId, request.bandId, request.occurredAt)

        val response = saved.toSafetyResponse()
        eventPublisher.publishEvent(SafetyEventCreated(response))
        return response
    }

    private fun touchDeviceLastSeen(
        siteId: Long,
        deviceId: String?,
        bandId: String?,
        seenAt: java.time.LocalDateTime,
    ) {
        val key = deviceId ?: bandId ?: return
        val device = deviceRepository.findByDeviceIdAndSiteId(key, siteId) ?: return
        device.lastSeenAt = seenAt
    }
}
