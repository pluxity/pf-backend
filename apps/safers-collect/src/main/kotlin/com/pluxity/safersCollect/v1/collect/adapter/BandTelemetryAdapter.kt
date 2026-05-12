package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.forwarder.enums.SourceType
import com.pluxity.safersCollect.v1.collect.dto.BandCollectRequest
import org.springframework.stereotype.Component

@Component
class BandTelemetryAdapter {
    companion object {
        const val BAND_MEASUREMENT = "band_reading"
        const val BAND_ID = "band_id"
        const val WEAR_STATE = "wear_state"
        const val LAT = "lat"
        const val LON = "lon"
        const val ALT = "alt"
        const val ACCURACY_M = "accuracy_m"
        const val HEART_RATE_BPM = "heart_rate_bpm"
        const val SPO2 = "spo2"
        const val BODY_TEMP_C = "body_temp_c"
        const val STEP = "step"
        const val BATTERY = "battery"
    }

    fun toEnvelopes(request: BandCollectRequest): List<TelemetryEnvelope> =
        listOf(
            TelemetryEnvelope(
                sourceType = SourceType.BAND,
                sourceId = request.bandId,
                timestamp = request.timestamp,
                measurement = BAND_MEASUREMENT,
                tags =
                    buildMap {
                        put(BAND_ID, request.bandId)
                        request.wearState?.let { put(WEAR_STATE, it.name) }
                    },
                fields =
                    buildMap {
                        request.rawPosition?.let { position ->
                            put(LAT, position.lat)
                            put(LON, position.lon)
                            position.alt?.let { put(ALT, it) }
                            position.accuracyM?.let { put(ACCURACY_M, it) }
                        }
                        request.vitals?.let { vital ->
                            vital.heartRateBpm?.let { put(HEART_RATE_BPM, it) }
                            vital.spo2?.let { put(SPO2, it) }
                            vital.bodyTempC?.let { put(BODY_TEMP_C, it) }
                            vital.step?.let { put(STEP, it) }
                        }
                        request.battery?.let { put(BATTERY, it) }
                    },
            ),
        )
}
