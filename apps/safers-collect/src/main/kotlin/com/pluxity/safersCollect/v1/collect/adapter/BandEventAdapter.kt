package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.forwarder.dto.WireRawPosition
import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.BandAbnormal
import com.pluxity.safersCollect.v1.collect.dto.BandEventRequest
import com.pluxity.safersCollect.v1.collect.dto.RawPosition
import com.pluxity.safersCollect.v1.collect.enums.BandEventType
import org.springframework.stereotype.Component

@Component
class BandEventAdapter {
    fun toEnvelope(request: BandEventRequest): EventEnvelope =
        EventEnvelope(
            eventId = request.eventId,
            eventType = request.eventType.toWire(),
            severity = request.severity,
            source = WireEventSource.SMART_BAND,
            occurredAt = request.occurredAt,
            bandId = request.payload.bandId,
            rawPosition = request.rawPosition?.toWire(),
            payload =
                buildMap {
                    request.payload.abnormal
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { put(ABNORMAL, it.map { item -> item.toMap() }) }
                    request.payload.impactG?.let { put(IMPACT_G, it) }
                    request.payload.lastSeenAt?.let { put(LAST_SEEN_AT, it) }
                },
        )

    private fun BandEventType.toWire(): WireEventType =
        when (this) {
            BandEventType.BAND_VITAL_ABNORMAL -> WireEventType.BAND_VITAL_ABNORMAL
            BandEventType.BAND_FALL_DETECTED -> WireEventType.BAND_FALL_DETECTED
            BandEventType.BAND_OFFLINE -> WireEventType.BAND_OFFLINE
        }

    private fun RawPosition.toWire(): WireRawPosition = WireRawPosition(lat = lat, lon = lon, alt = alt, accuracyM = accuracyM)

    private fun BandAbnormal.toMap(): Map<String, Any> =
        buildMap {
            put(METRIC, metric.name)
            put(VALUE, value)
            put(UNIT, unit)
            thresholdHigh?.let { put(THRESHOLD_HIGH, it) }
            thresholdLow?.let { put(THRESHOLD_LOW, it) }
        }

    companion object {
        const val ABNORMAL = "abnormal"
        const val IMPACT_G = "impactG"
        const val LAST_SEEN_AT = "lastSeenAt"
        const val METRIC = "metric"
        const val VALUE = "value"
        const val UNIT = "unit"
        const val THRESHOLD_HIGH = "thresholdHigh"
        const val THRESHOLD_LOW = "thresholdLow"
    }
}
