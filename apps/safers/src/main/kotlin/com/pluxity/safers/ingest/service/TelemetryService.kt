package com.pluxity.safers.ingest.service

import com.pluxity.safers.ingest.dto.TelemetryRequest
import com.pluxity.safers.ingest.persistence.influx.InfluxTelemetryWriter
import org.springframework.stereotype.Service

@Service
class TelemetryService(
    private val writer: InfluxTelemetryWriter,
) {
    fun ingest(
        siteId: Long,
        samples: List<TelemetryRequest>,
    ) {
        samples.forEach { writer.write(siteId, it) }
    }
}
