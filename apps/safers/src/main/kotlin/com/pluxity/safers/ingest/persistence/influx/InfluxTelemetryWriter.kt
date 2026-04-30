package com.pluxity.safers.ingest.persistence.influx

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.pluxity.safers.ingest.config.InfluxProperties
import com.pluxity.safers.ingest.dto.TelemetryRequest
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class InfluxTelemetryWriter(
    private val writeApi: WriteApi,
    private val props: InfluxProperties,
) {
    fun write(
        siteId: Long,
        request: TelemetryRequest,
    ) {
        val point =
            Point
                .measurement(request.measurement)
                .addTags(request.tags)
                .addTag(SITE_ID_TAG, siteId.toString()) // path siteId 강제 — 위변조 차단
                .also { p -> request.fields.forEach { (k, v) -> addField(p, k, v) } }
                .time(request.timestamp.atZone(KST).toInstant(), WritePrecision.MS)

        writeApi.writePoint(props.bucket, props.org, point)
    }

    private fun addField(
        point: Point,
        key: String,
        value: Any,
    ) {
        when (value) {
            is Number -> point.addField(key, value)
            is Boolean -> point.addField(key, value)
            is String -> point.addField(key, value)
            else -> point.addField(key, value.toString())
        }
    }

    companion object {
        private const val SITE_ID_TAG = "site_id"
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
