package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.forwarder.dto.WireRawPosition
import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.BandVitals
import com.pluxity.safersCollect.v1.collect.dto.RawPosition
import com.pluxity.safersCollect.v1.collect.dto.SosEventRequest
import org.springframework.stereotype.Component

@Component
class SosEventAdapter {
    fun toEnvelope(request: SosEventRequest): EventEnvelope =
        EventEnvelope(
            eventId = request.eventId,
            eventType = WireEventType.SOS_TRIGGERED,
            severity = request.severity,
            source = WireEventSource.SOS_DEVICE,
            occurredAt = request.occurredAt,
            bandId = request.payload.bandId,
            rawPosition = request.rawPosition?.toWire(),
            payload =
                buildMap {
                    put(TRIGGER, request.payload.trigger.name)
                    request.payload.vitals
                        ?.toMap()
                        ?.let { put(VITALS, it) }
                    request.payload.memo?.let { put(MEMO, it) }
                },
        )

    private fun RawPosition.toWire(): WireRawPosition = WireRawPosition(lat = lat, lon = lon, alt = alt, accuracyM = accuracyM)

    private fun BandVitals.toMap(): Map<String, Any> =
        buildMap {
            heartRateBpm?.let { put(HEART_RATE_BPM, it) }
            bodyTempC?.let { put(BODY_TEMP_C, it) }
            spo2?.let { put(SPO2, it) }
            step?.let { put(STEP, it) }
        }

    companion object {
        const val TRIGGER = "trigger"
        const val VITALS = "vitals"
        const val MEMO = "memo"
        const val HEART_RATE_BPM = "heartRateBpm"
        const val BODY_TEMP_C = "bodyTempC"
        const val SPO2 = "spo2"
        const val STEP = "step"
    }
}
