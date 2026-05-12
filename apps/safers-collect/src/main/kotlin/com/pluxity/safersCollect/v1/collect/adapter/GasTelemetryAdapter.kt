package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.forwarder.enums.SourceType
import com.pluxity.safersCollect.v1.collect.dto.GasTelemetry
import org.springframework.stereotype.Component

@Component
class GasTelemetryAdapter {
    companion object {
        const val GAS_MEASUREMENT = "gas_reading"
        const val DEVICE_ID = "device_id"
        const val GAS_UNIT = "unit"
        const val GAS = "gas"
        const val GAS_VALUE = "value"
        const val GAS_BATTERY = "battery"
        const val GAS_SIGNAL_RSSI = "signal_rssi"
    }

    fun toEnvelopes(request: GasTelemetry): List<TelemetryEnvelope> =
        request.measurements.map { measurement ->
            TelemetryEnvelope(
                sourceId = request.deviceId,
                sourceType = SourceType.GAS,
                measurement = GAS_MEASUREMENT,
                tags =
                    mapOf(
                        DEVICE_ID to request.deviceId,
                        GAS_UNIT to measurement.unit.name,
                        GAS to measurement.gas.name,
                    ),
                fields =
                    buildMap {
                        put(GAS_VALUE, measurement.value)
                        request.battery?.let { put(GAS_BATTERY, it) }
                        request.signalRssi?.let { put(GAS_SIGNAL_RSSI, it) }
                    },
                timestamp = request.timestamp,
            )
        }
}
