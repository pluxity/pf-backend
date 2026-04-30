package com.pluxity.safers.ingest.persistence.influx

import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import com.pluxity.safers.ingest.config.InfluxProperties
import com.pluxity.safers.ingest.dto.SourceType
import com.pluxity.safers.ingest.dto.TelemetryRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.time.ZoneId

class InfluxTelemetryWriterTest :
    BehaviorSpec({

        val writeApi: WriteApi = mockk(relaxed = true)
        val props =
            InfluxProperties(
                url = "http://localhost:8086",
                token = "pluxity",
                org = "pluxity",
                bucket = "safety",
            )
        val writer = InfluxTelemetryWriter(writeApi, props)

        Given("정상 가스 telemetry 요청") {
            val request =
                TelemetryRequest(
                    sourceType = SourceType.GAS,
                    sourceId = "GAS-MH203-01",
                    timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                    measurement = "gas_reading",
                    tags =
                        mapOf(
                            "device_id" to "GAS-MH203-01",
                            "gas" to "H2S",
                            "unit" to "PPM",
                        ),
                    fields =
                        mapOf(
                            "value" to 3.1,
                            "battery" to 87,
                        ),
                )

            When("siteId=42 로 write 호출") {
                writer.write(siteId = 42L, request = request)

                Then("InfluxProperties 의 bucket / org 로 writePoint 호출") {
                    verify { writeApi.writePoint("safety", "pluxity", any()) }
                }

                Then("line protocol 에 measurement / tags / fields 가 모두 들어감") {
                    val captured = slot<Point>()
                    verify { writeApi.writePoint(any(), any(), capture(captured)) }
                    val line = captured.captured.toLineProtocol()

                    line shouldContain "gas_reading"
                    line shouldContain "site_id=42"
                    line shouldContain "device_id=GAS-MH203-01"
                    line shouldContain "gas=H2S"
                    line shouldContain "unit=PPM"
                    line shouldContain "value=3.1"
                    line shouldContain "battery=87i"
                }
            }
        }

        Given("request.tags 에 site_id 가 위변조로 들어온 경우") {
            val request =
                TelemetryRequest(
                    sourceType = SourceType.GAS,
                    sourceId = "GAS-MH203-01",
                    timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                    measurement = "gas_reading",
                    tags =
                        mapOf(
                            "site_id" to "99", // ← 위변조 시도
                            "gas" to "H2S",
                        ),
                    fields = mapOf("value" to 3.1),
                )

            When("siteId=42 로 write 호출") {
                writer.write(siteId = 42L, request = request)

                Then("path siteId(42)가 우선되어 위변조가 차단됨") {
                    val captured = slot<Point>()
                    verify { writeApi.writePoint(any(), any(), capture(captured)) }
                    val line = captured.captured.toLineProtocol()

                    line shouldContain "site_id=42"
                    line shouldNotContain "site_id=99"
                }
            }
        }

        Given("다양한 field 타입") {
            val request =
                TelemetryRequest(
                    sourceType = SourceType.BAND,
                    sourceId = "BAND-A1B2C3",
                    timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                    measurement = "band_reading",
                    tags = emptyMap(),
                    fields =
                        mapOf(
                            "intVal" to 88,
                            "longVal" to 1_000L,
                            "doubleVal" to 36.7,
                            "boolVal" to true,
                            "stringVal" to "WORN",
                        ),
                )

            When("write 호출") {
                writer.write(siteId = 1L, request = request)

                Then("Int / Long → 'i' 접미사, Double → 그대로, Boolean → true/false, String → 따옴표") {
                    val captured = slot<Point>()
                    verify { writeApi.writePoint(any(), any(), capture(captured)) }
                    val line = captured.captured.toLineProtocol()

                    line shouldContain "intVal=88i"
                    line shouldContain "longVal=1000i"
                    line shouldContain "doubleVal=36.7"
                    line shouldContain "boolVal=true"
                    line shouldContain "stringVal=\"WORN\""
                }
            }
        }

        Given("KST timestamp") {
            val kstTimestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30) // KST 10:15:30
            val request =
                TelemetryRequest(
                    sourceType = SourceType.GAS,
                    sourceId = "GAS-001",
                    timestamp = kstTimestamp,
                    measurement = "gas_reading",
                    tags = emptyMap(),
                    fields = mapOf("value" to 1.0),
                )

            When("write 호출") {
                writer.write(siteId = 1L, request = request)

                Then("KST → UTC Instant 변환 (9시간 차) 후 ms 정밀도로 직렬화") {
                    val captured = slot<Point>()
                    verify { writeApi.writePoint(any(), any(), capture(captured)) }
                    val line = captured.captured.toLineProtocol()

                    val expectedMillis =
                        kstTimestamp
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant()
                            .toEpochMilli()
                    line shouldContain expectedMillis.toString()
                }
            }
        }
    })
