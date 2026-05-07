package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.BandVitals
import com.pluxity.safersCollect.v1.collect.dto.RawPosition
import com.pluxity.safersCollect.v1.collect.dto.SosEventPayload
import com.pluxity.safersCollect.v1.collect.dto.SosEventRequest
import com.pluxity.safersCollect.v1.collect.enums.EventSeverity
import com.pluxity.safersCollect.v1.collect.enums.SosTrigger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class SosEventAdapterTest :
    BehaviorSpec({

        val adapter = SosEventAdapter()

        Given("BUTTON_LONG_PRESS + vitals + memo + rawPosition (alt 포함) 모두 채움") {
            val occurredAt = LocalDateTime.of(2026, 4, 24, 10, 15, 30)
            val request =
                SosEventRequest(
                    eventId = "SOS-EVT-20260424-0001",
                    severity = EventSeverity.CRITICAL,
                    occurredAt = occurredAt,
                    rawPosition =
                        RawPosition(
                            lat = 37.5665,
                            lon = 126.9780,
                            alt = 12.5,
                            accuracyM = 3.5,
                        ),
                    payload =
                        SosEventPayload(
                            bandId = "BAND-A1B2C3",
                            trigger = SosTrigger.BUTTON_LONG_PRESS,
                            vitals =
                                BandVitals(
                                    heartRateBpm = 110,
                                    bodyTempC = 36.7,
                                    spo2 = 96,
                                    step = 4321,
                                ),
                            memo = "현장 추락 의심",
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("top-level wire 매핑") {
                    envelope.eventId shouldBe "SOS-EVT-20260424-0001"
                    envelope.eventType shouldBe WireEventType.SOS_TRIGGERED
                    envelope.severity shouldBe EventSeverity.CRITICAL
                    envelope.source shouldBe WireEventSource.SOS_DEVICE
                    envelope.occurredAt shouldBe occurredAt
                    envelope.bandId shouldBe "BAND-A1B2C3"
                    envelope.deviceId shouldBe null
                }
                Then("rawPosition 의 alt / accuracyM 이 wire 로 통과 (lng→lon 통일)") {
                    val rawPosition = envelope.rawPosition!!
                    rawPosition.lat shouldBe 37.5665
                    rawPosition.lon shouldBe 126.9780
                    rawPosition.alt shouldBe 12.5
                    rawPosition.accuracyM shouldBe 3.5
                }
                Then("payload 에 trigger / vitals / memo 모두 존재") {
                    envelope.payload.keys shouldBe setOf("trigger", "vitals", "memo")
                    envelope.payload["trigger"] shouldBe "BUTTON_LONG_PRESS"
                    envelope.payload["memo"] shouldBe "현장 추락 의심"
                    @Suppress("UNCHECKED_CAST")
                    val vitals = envelope.payload["vitals"] as Map<String, Any>
                    vitals.keys shouldBe setOf("heartRateBpm", "bodyTempC", "spo2", "step")
                }
            }
        }

        Given("MOBILE_APP trigger, vitals/memo/rawPosition 모두 null") {
            val request =
                SosEventRequest(
                    eventId = "SOS-EVT-20260424-0002",
                    severity = EventSeverity.CRITICAL,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 16, 0),
                    rawPosition = null,
                    payload =
                        SosEventPayload(
                            bandId = "BAND-X9Y8Z7",
                            trigger = SosTrigger.MOBILE_APP,
                            vitals = null,
                            memo = null,
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("rawPosition 은 null 그대로") {
                    envelope.rawPosition shouldBe null
                }
                Then("payload 에 trigger 만 존재 (vitals/memo 키 자체 없음)") {
                    envelope.payload.keys shouldBe setOf("trigger")
                    envelope.payload["trigger"] shouldBe "MOBILE_APP"
                }
            }
        }

        Given("vitals 일부 (heartRateBpm) 만 채워진 경우") {
            val request =
                SosEventRequest(
                    eventId = "SOS-EVT-20260424-0003",
                    severity = EventSeverity.WARNING,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 10, 17, 0),
                    payload =
                        SosEventPayload(
                            bandId = "BAND-PARTIAL",
                            trigger = SosTrigger.MANUAL_DISPATCH,
                            vitals = BandVitals(heartRateBpm = 95),
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("vitals 안에 heartRateBpm 만 있음 — null 필드는 키 자체 누락") {
                    @Suppress("UNCHECKED_CAST")
                    val vitals = envelope.payload["vitals"] as Map<String, Any>
                    vitals shouldBe mapOf("heartRateBpm" to 95)
                }
            }
        }
    })
