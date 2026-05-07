package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.dto.GasEventPayload
import com.pluxity.safersCollect.v1.collect.dto.GasEventRequest
import com.pluxity.safersCollect.v1.collect.dto.GasTriggered
import com.pluxity.safersCollect.v1.collect.enums.EventSeverity
import com.pluxity.safersCollect.v1.collect.enums.GasType
import com.pluxity.safersCollect.v1.collect.enums.GasUnit
import com.pluxity.safersCollect.v1.collect.enums.ThresholdLevel
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GasEventAdapterTest :
    BehaviorSpec({

        val adapter = GasEventAdapter()

        Given("CO 1건 임계 초과 이벤트") {
            val occurredAt = LocalDateTime.of(2026, 4, 24, 10, 15, 30)
            val request =
                GasEventRequest(
                    eventId = "GAS-EVT-20260424-0001",
                    severity = EventSeverity.WARNING,
                    occurredAt = occurredAt,
                    payload =
                        GasEventPayload(
                            deviceId = "GAS-MH203-01",
                            triggered =
                                listOf(
                                    GasTriggered(
                                        gas = GasType.CO,
                                        value = 50.0,
                                        unit = GasUnit.PPM,
                                        threshold = 30.0,
                                        level = ThresholdLevel.WARNING,
                                    ),
                                ),
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("top-level 매핑이 wire 호환") {
                    envelope.eventId shouldBe "GAS-EVT-20260424-0001"
                    envelope.eventType shouldBe WireEventType.GAS_THRESHOLD_EXCEEDED
                    envelope.severity shouldBe EventSeverity.WARNING
                    envelope.source shouldBe WireEventSource.GAS_SENSOR
                    envelope.occurredAt shouldBe occurredAt
                    envelope.deviceId shouldBe "GAS-MH203-01"
                    envelope.bandId shouldBe null
                    envelope.rawPosition shouldBe null
                }
                Then("payload.triggered 가 List<Map> 형태") {
                    envelope.payload shouldContainKey "triggered"
                    @Suppress("UNCHECKED_CAST")
                    val triggered = envelope.payload["triggered"] as List<Map<String, Any>>
                    triggered.size shouldBe 1
                    triggered[0]["gas"] shouldBe "CO"
                    triggered[0]["value"] shouldBe 50.0
                    triggered[0]["unit"] shouldBe "PPM"
                    triggered[0]["threshold"] shouldBe 30.0
                    triggered[0]["level"] shouldBe "WARNING"
                }
            }
        }

        Given("동시 다중 가스 임계 초과 (CO + H2S)") {
            val request =
                GasEventRequest(
                    eventId = "GAS-EVT-MULTI-001",
                    severity = EventSeverity.CRITICAL,
                    occurredAt = LocalDateTime.of(2026, 4, 24, 11, 0, 0),
                    payload =
                        GasEventPayload(
                            deviceId = "GAS-MH203-01",
                            triggered =
                                listOf(
                                    GasTriggered(GasType.CO, 80.0, GasUnit.PPM, 30.0, ThresholdLevel.DANGER),
                                    GasTriggered(GasType.H2S, 15.0, GasUnit.PPM, 10.0, ThresholdLevel.WARNING),
                                ),
                        ),
                )

            When("toEnvelope 호출") {
                val envelope = adapter.toEnvelope(request)

                Then("triggered List 가 2건 모두 보존") {
                    @Suppress("UNCHECKED_CAST")
                    val triggered = envelope.payload["triggered"] as List<Map<String, Any>>
                    triggered.size shouldBe 2
                    triggered.map { it["gas"] } shouldBe listOf("CO", "H2S")
                }
            }
        }
    })
