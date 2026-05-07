package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.BandAbnormal
import com.pluxity.safersCollect.v1.collect.dto.BandEventPayload
import com.pluxity.safersCollect.v1.collect.dto.BandEventRequest
import com.pluxity.safersCollect.v1.collect.dto.RawPosition
import com.pluxity.safersCollect.v1.collect.enums.BandEventType
import com.pluxity.safersCollect.v1.collect.enums.EventSeverity
import com.pluxity.safersCollect.v1.collect.enums.VitalMetric
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class BandEventAdapterTest :
    BehaviorSpec({

        val adapter = BandEventAdapter()

        Given("BAND_VITAL_ABNORMAL — abnormal[] 만 채움") {
            val occurredAt = LocalDateTime.of(2026, 4, 24, 10, 15, 30)
            val request =
                BandEventRequest(
                    eventId = "BAND-EVT-VITAL-001",
                    eventType = BandEventType.BAND_VITAL_ABNORMAL,
                    severity = EventSeverity.WARNING,
                    occurredAt = occurredAt,
                    rawPosition =
                        RawPosition(
                            lat = 37.5665,
                            lon = 126.9780,
                            alt = 8.0,
                        ),
                    payload =
                        BandEventPayload(
                            bandId = "BAND-A1B2C3",
                            abnormal =
                                listOf(
                                    BandAbnormal(
                                        metric = VitalMetric.HEART_RATE,
                                        value = 150.0,
                                        unit = "BPM",
                                        thresholdHigh = 140.0,
                                    ),
                                ),
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("eventType 매핑 + 공통 wire 필드") {
                    envelope.eventType shouldBe WireEventType.BAND_VITAL_ABNORMAL
                    envelope.source shouldBe WireEventSource.SMART_BAND
                    envelope.bandId shouldBe "BAND-A1B2C3"
                    envelope.rawPosition!!.alt shouldBe 8.0
                }
                Then("payload 에 abnormal 만 (impactG / lastSeenAt 키 없음)") {
                    envelope.payload.keys shouldBe setOf("abnormal")
                    @Suppress("UNCHECKED_CAST")
                    val abnormal = envelope.payload["abnormal"] as List<Map<String, Any>>
                    abnormal[0] shouldBe
                        mapOf(
                            "metric" to "HEART_RATE",
                            "value" to 150.0,
                            "unit" to "BPM",
                            "thresholdHigh" to 140.0,
                        )
                }
            }
        }

        Given("BAND_FALL_DETECTED — impactG 만 채움") {
            val request =
                BandEventRequest(
                    eventId = "BAND-EVT-FALL-001",
                    eventType = BandEventType.BAND_FALL_DETECTED,
                    severity = EventSeverity.CRITICAL,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 16, 0),
                    payload =
                        BandEventPayload(
                            bandId = "BAND-A1B2C3",
                            impactG = 4.2,
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("eventType BAND_FALL_DETECTED + payload 에 impactG 만") {
                    envelope.eventType shouldBe WireEventType.BAND_FALL_DETECTED
                    envelope.payload shouldBe mapOf("impactG" to 4.2)
                }
            }
        }

        Given("BAND_OFFLINE — lastSeenAt LocalDateTime 보존") {
            val lastSeen = LocalDateTime.of(2026, 4, 24, 10, 10, 0)
            val request =
                BandEventRequest(
                    eventId = "BAND-EVT-OFFLINE-001",
                    eventType = BandEventType.BAND_OFFLINE,
                    severity = EventSeverity.WARNING,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 17, 0),
                    payload =
                        BandEventPayload(
                            bandId = "BAND-A1B2C3",
                            lastSeenAt = lastSeen,
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("lastSeenAt 가 LocalDateTime 그대로 (Jackson 이 ISO 직렬화 — toString 변환 X)") {
                    envelope.eventType shouldBe WireEventType.BAND_OFFLINE
                    envelope.payload.keys shouldBe setOf("lastSeenAt")
                    envelope.payload["lastSeenAt"] shouldBe lastSeen
                }
            }
        }

        Given("abnormal 이 빈 리스트") {
            val request =
                BandEventRequest(
                    eventId = "BAND-EVT-EMPTY-001",
                    eventType = BandEventType.BAND_VITAL_ABNORMAL,
                    severity = EventSeverity.INFO,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 18, 0),
                    payload =
                        BandEventPayload(
                            bandId = "BAND-A1B2C3",
                            abnormal = emptyList(),
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("abnormal 키 자체가 누락 (takeIf isNotEmpty)") {
                    envelope.payload.containsKey("abnormal") shouldBe false
                }
            }
        }

        Given("rawPosition / abnormal / impactG / lastSeenAt 모두 null") {
            val request =
                BandEventRequest(
                    eventId = "BAND-EVT-MIN-001",
                    eventType = BandEventType.BAND_OFFLINE,
                    severity = EventSeverity.INFO,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 19, 0),
                    rawPosition = null,
                    payload = BandEventPayload(bandId = "BAND-A1B2C3"),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("rawPosition null + payload 빈 Map") {
                    envelope.rawPosition shouldBe null
                    envelope.payload shouldBe emptyMap()
                }
            }
        }
    })
