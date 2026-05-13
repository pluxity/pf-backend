package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.v1.collect.adapter.GasEventAdapter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class GasEventMqttHandlerTest :
    BehaviorSpec({

        val objectMapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        val adapter = GasEventAdapter()
        val eventForwarder = mockk<EventForwarder>(relaxed = true)
        val handler = GasEventMqttHandler(objectMapper, adapter, eventForwarder)

        Given("GasEventRequest JSON 페이로드") {
            val json =
                """
                {
                  "eventId": "GAS-EVT-20260424-0001",
                  "severity": "CRITICAL",
                  "occurredAt": "2026-04-24T10:15:30",
                  "payload": {
                    "deviceId": "GAS-MH203-01",
                    "triggered": [
                      {"gas": "O2", "value": 3.1, "unit": "PPM", "threshold": 5.0, "level": "DANGER"}
                    ]
                  }
                }
                """.trimIndent().toByteArray()

            When("handle 호출") {
                handler.handle(json)

                Then("topicSuffix 는 gas/events") {
                    handler.topicSuffix shouldBe "gas/events"
                }

                Then("EventForwarder.forward 가 envelope 1개로 호출됨") {
                    val captured = slot<EventEnvelope>()
                    verify(exactly = 1) { eventForwarder.forward(capture(captured)) }
                    captured.captured.eventId shouldBe "GAS-EVT-20260424-0001"
                }
            }
        }
    })
