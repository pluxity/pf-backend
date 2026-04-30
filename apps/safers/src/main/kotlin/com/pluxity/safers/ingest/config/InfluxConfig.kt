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
    fun writeApi(
        client: InfluxDBClient,
        props: InfluxProperties,
    ): WriteApi {
        val writeApi =
            client.makeWriteApi(
                WriteOptions
                    .builder()
                    .batchSize(props.batchSize)
                    .flushInterval(props.flushIntervalMs)
                    .bufferLimit(props.bufferLimit)
                    .retryInterval(props.retryIntervalMs)
                    .maxRetries(props.maxRetries)
                    .build(),
            )

        writeApi.listenEvents(WriteErrorEvent::class.java) { event ->
            log.error(event.throwable) { "[InfluxDB] write failed: ${event.throwable.message}" }
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
