package com.pluxity.safers.ingest.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApi
import com.influxdb.client.WriteOptions
import com.influxdb.client.write.events.BackpressureEvent
import com.influxdb.client.write.events.WriteErrorEvent
import com.influxdb.client.write.events.WriteRetriableErrorEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(InfluxProperties::class)
class InfluxConfig {
    @Bean(destroyMethod = "close")
    fun influxClient(props: InfluxProperties): InfluxDBClient =
        InfluxDBClientFactory.create(
            props.url,
            props.token.toCharArray(),
            props.org,
            props.bucket,
        )

    @Bean(destroyMethod = "close")
    fun writeApi(client: InfluxDBClient): WriteApi {
        val writeApi =
            client.makeWriteApi(
                WriteOptions
                    .builder()
                    .batchSize(1000) // 1000건 모이면 flush
                    .flushInterval(1000) // 또는 1초마다 강제 flush (먼저 도달)
                    .bufferLimit(10_000) // buffer 1만 건 초과 시 백프레셔
                    .retryInterval(5_000) // 실패 시 5초 후 재시도
                    .maxRetries(3)
                    .build(),
            )

        writeApi.listenEvents(WriteErrorEvent::class.java) {
            log.error { "[InfluxDB] write failed (final)" }
            // dead-letter?
        }
        writeApi.listenEvents(WriteRetriableErrorEvent::class.java) {
            log.warn { "[InfluxDB] write retriable error — will retry" }
        }
        writeApi.listenEvents(BackpressureEvent::class.java) {
            log.warn { "[InfluxDB] backpressure — buffer 임계 도달, 송신 속도 ↓" }
        }

        return writeApi
    }
}
