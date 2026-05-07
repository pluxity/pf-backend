package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.GasEventRequest
import com.pluxity.safersCollect.v1.collect.dto.GasTriggered
import org.springframework.stereotype.Component

@Component
class GasEventAdapter {
    fun toEnvelope(request: GasEventRequest): EventEnvelope =
        EventEnvelope(
            eventId = request.eventId,
            eventType = WireEventType.GAS_THRESHOLD_EXCEEDED,
            severity = request.severity,
            source = WireEventSource.GAS_SENSOR,
            occurredAt = request.occurredAt,
            deviceId = request.payload.deviceId,
            payload = mapOf(TRIGGERED to request.payload.triggered.map { it.toMap() }),
        )

    private fun GasTriggered.toMap(): Map<String, Any> =
        mapOf(
            GAS to gas.name,
            VALUE to value,
            UNIT to unit.name,
            THRESHOLD to threshold,
            LEVEL to level.name,
        )

    companion object {
        const val TRIGGERED = "triggered"
        const val GAS = "gas"
        const val VALUE = "value"
        const val UNIT = "unit"
        const val THRESHOLD = "threshold"
        const val LEVEL = "level"
    }
}
